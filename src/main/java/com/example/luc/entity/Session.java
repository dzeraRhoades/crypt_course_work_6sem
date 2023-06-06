package com.example.luc.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Session implements Runnable {
    private Gson gson;

    private Path sessionDir;
    private final Socket firstClient;
    private final Socket secondClient;
    Scanner scannerFirst = null;
    Scanner scannerSecond = null;
    PrintWriter outputFirst = null;
    PrintWriter outputSecond = null;
    private byte[] asymKey;
    private byte[] symKey;
    private final Map<String, String> files = new ConcurrentHashMap<>();

    public Session(Socket first, Socket second)
    {
        try {
            sessionDir = Files.createTempDirectory("session");
            scannerFirst = new Scanner(first.getInputStream());
            scannerSecond = new Scanner(second.getInputStream());
            outputFirst = new PrintWriter(first.getOutputStream(), true);
            outputSecond = new PrintWriter(second.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        gson = new GsonBuilder().create();
        firstClient = first;
        secondClient = second;
        System.out.println("Session is successfully init!");
    }

    @Override
    public void run() {
        // сначала нужно распределить ключи
        Message genAsymKey = new Message("key_request", List.of("asym"));
        Message genSymKey = new Message("key_request", List.of("sym"));
        sendMessage(genAsymKey, CLIENT.FIRST);
        sendMessage(genSymKey, CLIENT.SECOND);
        System.out.println("requests are sent to clients");
        // ждем асиметричного ключа от первого
        if(scannerFirst.hasNext())
        {
            Message msg = parseMessage(scannerFirst.nextLine());
            asymKey = ((String) msg.data.get(0)).getBytes();
            System.out.println("got asym key from first client: " + asymKey);
        }
        // отправляем его второму
        if(asymKey == null)
            throw new RuntimeException("asym key is null");
        Message asym = new Message("key", List.of(new String(asymKey)));
        sendMessage(asym, CLIENT.SECOND);
        System.out.println("sent asym key to second");
        // получаем от второго зашифрованный симметричный ключ
        if(scannerSecond.hasNext())
        {
            Message msg = parseMessage(scannerSecond.nextLine());
            symKey = ((String)msg.data.get(0)).getBytes();
            System.out.println("got ecnrypt sym key from second: " + Arrays.toString(symKey));
        }
        // отправляем первому
        Message sym = new Message("key", List.of(new String(symKey)));
        sendMessage(sym, CLIENT.FIRST);
        System.out.println("sent sym key to first");

        // теперь ключи распределены, создаём два потока для каждого клиента и принимаем их запросы на загрузку
        // и скачивание файла
        // храним в files закаченные на сервер файлы

        Thread thread1 = new Thread(()->clientListener(CLIENT.FIRST));
        thread1.start();
        Thread thread2 = new Thread(()->clientListener(CLIENT.SECOND));
        thread2.start();
        // ждем когда хотябы один из потоков завершится
        while (thread1.isAlive() && thread2.isAlive())
        {
            Thread.yield();
        }
        sendMessage(new Message("disconnect", List.of()), (thread1.isAlive() ? CLIENT.FIRST : CLIENT.SECOND));
//        try {
//            Files.deleteIfExists(sessionDir);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
    }

    public void clientListener(CLIENT client)
    {
        Scanner scanner = client.equals(CLIENT.FIRST) ? scannerFirst : scannerSecond;
        while (scanner.hasNext() && !Thread.currentThread().isInterrupted())
        {
            Message msg = parseMessage(scanner.nextLine());
            if(msg.type.equals("download"))
            {
                System.out.println("got request to download file");
                String filename = (String) msg.data.get(0);
                if(files.containsKey(filename))
                {
                    downloadFile(filename, client);
                }
                else
                {
                    sendMessage(new Message("fail", List.of("doesn't exist")), client);
                }
            }
            else if(msg.type.equals("load"))
            {
                System.out.println("got request to load file");
                String filename = (String) msg.data.get(0);
                if(files.containsKey(filename))
                {
                    sendMessage(new Message("fail", List.of("file with this name already exist")), client);
                }
                else
                {
                    sendMessage(new Message("ok", List.of("")), client);
                    if(loadFile(filename, client))
                        files.put(filename, "");
                }
            }
            else
            {
                System.out.println("operation " + msg.type + " is not supported");
            }
        }

    }

    private boolean downloadFile(String filename, CLIENT output)
    {
//        if(!files.containsKey(filename))
//        {
//            sendMessage(new Message("fail", List.of("file " + filename + " doesn't exist")), client);
//            return false;
//        }
        Path file = sessionDir.resolve(filename);

        final int BUFSIZ = 1024 * 1024; // Mb
        byte[] buf;
        try (InputStream inputStream = new FileInputStream(file.toString()))
        {
            System.out.println("start downloading file");
            do
            {
                System.out.println("still downloading file...");

                buf = inputStream.readNBytes(BUFSIZ);
                byte[] encodedBuf = Base64.getEncoder().encode(buf);
                if(buf.length != 0)
                    sendMessage(new Message("downloading", List.of(new String(encodedBuf))), output);
            }
            while(buf.length == BUFSIZ);
            sendMessage(new Message("finished", List.of()), output);
            System.out.println("finished downloading file");
        }
        catch (FileNotFoundException e)
        {
            sendMessage(new Message("fail", List.of("file " + filename + " doesn't exist")), output);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    private boolean loadFile(String filename, CLIENT client)
    {
        Path file = sessionDir.resolve(filename);
//        if(Files.exists(file))
//        {
//            sendMessage(new Message("fail", List.of("file " + filename + " already exists")), client);
//            return false;
//        }
        byte[] buf;
        Scanner scanner = client.equals(CLIENT.FIRST) ? scannerFirst : scannerSecond;
        try (OutputStream outputStream = new FileOutputStream(file.toString()))
        {
            System.out.println("start loading file");

            while(scanner.hasNext() && !Thread.currentThread().isInterrupted())
            {
                Message msg = parseMessage(scanner.nextLine());
                if(msg.type.equals("loading"))
                {
                    System.out.println("still loading file...");
                    byte[] decodedBuf = Base64.getDecoder().decode(((String) msg.data.get(0)).getBytes());
                    outputStream.write(decodedBuf);
                }
                else if(msg.type.equals("finished"))
                {
                    System.out.println("finished loading file");
                    break;
                }
                else
                {
                    // some errors occurred, so we need to remove this file
                    outputStream.close();
                    Files.deleteIfExists(file);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    void sendMessage(Message msg, CLIENT client)
    {
        String message = gson.toJson(msg);
        PrintWriter pr = client.equals(CLIENT.FIRST) ? outputFirst : outputSecond;
        pr.println(message);
    }
    private Message parseMessage(String msg)
    {
        return gson.fromJson(msg, Message.class);
    }

    private enum CLIENT
    {
        FIRST,
        SECOND
    }
}

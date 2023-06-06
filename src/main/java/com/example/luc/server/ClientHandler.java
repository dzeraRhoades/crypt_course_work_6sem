package com.example.luc.server;

import com.example.luc.entity.Message;
import com.example.luc.entity.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private static final Map<String, Socket> waitingSessions = new ConcurrentHashMap<>();

    public final String CONNECTION = "connection";
    PrintWriter output = null;
    Scanner input = null;

    private Gson gson;
    private Socket client;
    public ClientHandler(Socket client)
    {
        try {
            input = new Scanner(client.getInputStream());
            output = new PrintWriter(client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        gson = new GsonBuilder().create();
        this.client = client;
    }

    @Override
    public void run() {
        // подключился пользователь
        try {
            //Scanner input = new Scanner(client.getInputStream());
            // принимаем адрес клиента, с которым хочет взаимодействовать другой клиент
            if(input.hasNext())
            {

                // проверяем, ждет ли нас кто-то

                String address = client.getInetAddress().getHostAddress() + ":" + client.getPort();
                Message msg = parseMessage(input.nextLine());
                String[] names = ((String) msg.data.get(0)).split(":"); // host + port
                String clientName = names[0];
                String buddiesName = names[1];
//                System.out.println("адрес клиента, с которым хочет взаимодействовать другой клиент: "+ buddiesAddress);
//                System.out.println("адрес клиента: " + address);
                if(waitingSessions.containsKey(buddiesName + ":" + clientName))
                {
                    System.out.println("find pair!!!");
                    Socket waitingBuddySocket = waitingSessions.get(buddiesName + ":" + clientName);
                    Session session = new Session(waitingBuddySocket, client);
                    waitingSessions.remove(address);
                    Thread thread = new Thread(session);
                    thread.start();
                    thread.join();
                }
                else
                {
                    waitingSessions.put(clientName + ":" + buddiesName, client);
                    //sendMessage(new Message("wait", List.of("hell")), client);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void sendMessage(Message msg, Socket client)
    {
        output.println(new GsonBuilder().create().toJson(msg));
    }
    private Message parseMessage(String msg)
    {
        return gson.fromJson(msg, Message.class);
    }
}

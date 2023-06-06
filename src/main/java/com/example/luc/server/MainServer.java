package com.example.luc.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    private String host = "localhost";
    private Integer port = 8843;

    public MainServer(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public void start()
    {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (!Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(client);
                System.out.println("Клиент подключился");

                new Thread(clientHandler).start();
            }
        } catch (IOException ex) {

        }
    }
}

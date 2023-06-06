package com.example.luc;

import com.example.luc.server.MainServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerEntryPoint {
    public static void main(String[] args)
    {
        ExecutorService exc = Executors.newFixedThreadPool(2);
        MainServer server = new MainServer("localhost", 8433);
        exc.execute(server::start);
    }
}

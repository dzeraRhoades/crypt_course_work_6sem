package com.example.luc.server;

public class ServerException extends RuntimeException{
    private String what = "ServerException";
    private Exception parentEx = null;
    public ServerException(String msg, Exception ex)
    {
        this.what = msg;
        this.parentEx = ex;
    }
    public ServerException()
    {
    }
}

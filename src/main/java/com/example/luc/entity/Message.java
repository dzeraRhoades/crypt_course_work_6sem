package com.example.luc.entity;

import java.util.List;

public class Message
{
    public Message(String type, List<Object> data){
        this.type = type;
        this.data = data;
    }
    public String type;
    public List<Object> data;
}

package com.example.luc;

import java.util.function.Consumer;

public class TestConsumer {
    public void doSth(Consumer<Integer> consumer)
    {
        for(int i = 0; i < 100; ++i)
        {
            consumer.accept(i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

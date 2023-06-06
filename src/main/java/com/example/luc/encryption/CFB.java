package com.example.luc.encryption;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

public class CFB
{
    @Getter
    private volatile double progress;
    @Setter
    Consumer<Double> consumer;

    Camellia camellia;
    public void encrypt(InputStream inputStream, OutputStream outputStream, int blockSize, Camellia camellia, byte[] iv) throws InterruptedException {
        int readBytes = 0;
        byte[] buf = null;
        byte[] res = new byte[blockSize];
        //byte[] iv = new byte[blockSize];
        try {
            long streamSize = inputStream.available();
            long encryptedData = 0;
            while (!Thread.currentThread().isInterrupted())
            {
                buf = inputStream.readNBytes(blockSize);
                encryptedData += buf.length;
                progress = (double) encryptedData / streamSize;
                if(consumer != null)
                    consumer.accept(progress);
//                System.out.println("block: " + Arrays.toString(buf));
                System.out.println("Осталосб зашифровать: " + inputStream.available());
                if(inputStream.available() == 0)
                    break;
                camellia.processBlock(iv, 0, res, 0);
                res = xor(res, buf);
//                System.out.println("encr ecnrypted block: " + Arrays.toString(res));
                iv = res.clone();
                outputStream.write(res);
            }
            if(Thread.interrupted())
                throw new InterruptedException();
            if(buf != null && buf.length != 0)
            {
                buf = addPadding(buf, blockSize);

                // we need loop in case we added full block size (if buf % blockSize = 0)
                for(int i = 0; i < buf.length / blockSize; i++)
                {
                    byte[] tmp = Arrays.copyOfRange(buf, i * blockSize, (i+1) * blockSize);
                    camellia.processBlock(iv, 0, res, 0);
                    res = xor(res, tmp);
                    iv = res.clone();
//                    System.out.println("ecr ecnrypted block: " + Arrays.toString(res));

                    outputStream.write(res);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private byte[] xor(byte[] a, byte[] b)
    {
        byte[] res = new byte[Math.min(a.length, b.length)];
        for (int i = 0; i < Math.min(a.length, b.length); ++i)
        {
            res[i] = (byte) (a[i] ^ b[i]);
        }
        return res;
    }

    public void decrypt(InputStream inputStream, OutputStream outputStream, int blockSize, Camellia camellia, byte[] iv)
    {
        int readBytes = 0;
        byte[] buf = null;
        byte[] res = new byte[blockSize];
        //byte[] iv = new byte[blockSize];
        try {
            long streamSize = inputStream.available();
            long decryptedData = 0;
            while (true)
            {
                buf = inputStream.readNBytes(blockSize);
                decryptedData += buf.length;
                progress = (double) decryptedData / streamSize;
                if(consumer != null)
                    consumer.accept(progress);
//                System.out.println("block: " + Arrays.toString(buf));
                camellia.processBlock(iv, 0, res, 0);
                res = xor(res, buf);
//                System.out.println("decr ecnrypted block: " + Arrays.toString(res));
                iv = buf.clone();
                if(inputStream.available() == 0)
                    break;
                outputStream.write(res);
            }
            if(res.length != 0)
            {
                res = delPadding(res, blockSize);
                outputStream.write(res);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    private byte[] addPadding(byte[] buf, int blockSize)
    {
        int bytesToPad = blockSize - buf.length;
        if(bytesToPad == 0)
            bytesToPad = blockSize;
        byte[] res = new byte[buf.length + bytesToPad];
        for(int i = 0; i < buf.length; ++i)
        {
            res[i] = buf[i];
        }
        for(int i = buf.length; i < blockSize; ++i)
        {
            res[i] = (byte) bytesToPad;
        }
        return res;
    }
    private byte[] delPadding(byte[] buf, int blockSize)
    {
        int paddedBytes = buf[blockSize - 1];

        byte[] res = Arrays.copyOfRange(buf, 0, blockSize - paddedBytes);

        return res;
    }
}
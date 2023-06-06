package com.example.luc.encryption;

import javafx.scene.control.ListCell;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.function.BiFunction;

import static java.lang.Math.pow;

public class LUC {
    private static boolean dec = false;
    private BigInteger p;
    private BigInteger q;
    private BigInteger e;
    PublicKey publicKey;
    PrivateKey privateKey;
    public static List<BigInteger> lucValues = new ArrayList<>();
    public static BigInteger generateBigPrimeNumber()
    {
        return BigInteger.probablePrime(160, new Random());
    }
    public LUC()
    {
        init();
    }
    public static BigInteger generateCoprime(BigInteger num)
    {
        BigInteger start = new BigInteger(30, new Random());
        while(!start.gcd(num).equals(BigInteger.ONE) && start.compareTo(num) < 0)
        {
            System.out.println("num is not coprime: " + start);
            start = start.add(BigInteger.ONE);
        }
        if(!start.gcd(num).equals(BigInteger.ONE))
            throw new RuntimeException("can't generate coprime number");
        return start;
    }
    public static int legandre(BigInteger a, BigInteger p)
    {
        if(a.equals(BigInteger.ONE))
            return 1;

        // если число четное
        if(!a.testBit(0))
        {
            BigInteger division = p
                    .pow(2)
                    .subtract(BigInteger.ONE)
                    .divide(BigInteger.valueOf(8));
            int b = division.testBit(0) ? -1 : 1;
            return legandre(a.divide(BigInteger.TWO), p) * b;
        }
        else
        {
            BigInteger division = a.subtract(BigInteger.ONE).
                    multiply(p.subtract(BigInteger.ONE)).divide(BigInteger.valueOf(4));
            return legandre(p.mod(a), a) * (division.testBit(0) ? -1 : 1);
        }

    }
    private BigInteger lucasFast(BigInteger p, BigInteger n, BigInteger N)
    {
        boolean[] rems = getCount(n);
        BigInteger prev = BigInteger.TWO;
        BigInteger cur = p;
        BigInteger pInternal = p;
        BigInteger Ve;
        BigInteger Vo;

        for(int i = 0; i < rems.length; ++i)
        {
            boolean curRem = rems[i];
            if(curRem)
            {
                Ve = cur.pow(2).subtract(BigInteger.TWO);
                Vo = cur.multiply(prev).subtract(pInternal);
                prev = Vo.mod(N);
                cur = Ve.mod(N);
            }
            else
            {
                Vo = cur.pow(2).multiply(pInternal).subtract(cur.multiply(prev)).subtract(pInternal);
                prev = cur.pow(2).subtract(BigInteger.TWO).mod(N);
                cur = Vo.mod(N);
            }
        }
        return cur;
    }

    private boolean[] getCount(BigInteger n)
    {
        boolean[] count = new boolean[n.bitLength()];
        int i = 0;
        while(n.compareTo(BigInteger.ONE) > 0)
        {
            if(n.mod(BigInteger.TWO).equals(BigInteger.ZERO))
                count[i] = true;
            else
                count[i] = false;
            n = n.divide(BigInteger.TWO);
            i++;
        }
        return count;
    }
    /**
     * @param key public key
     */
    public byte[] encrypt(byte[] message, byte[] key)
    {
        String[] keyStr = new String(key).split(":");
        System.out.println();
        BigInteger ed = getBigIntFromKey(keyStr[0]);
       // BigInteger N = getBigIntFromKey(keyStr[1]);
        BigInteger msg = getBigIntFromKey(new String(message));
        //return lucSequence(msg, N, ed).toByteArray();
        return _lucSequence(message, key);
    }
    public byte[] decrypt(byte[] message, byte[] key)
    {
        return _decrypt(message, key);
    }
    public byte[] generatePublicKey()
    {
        p = BigInteger.probablePrime(32, new Random());
        q = BigInteger.probablePrime(32, new Random());
        BigInteger N = p.multiply(q);
        // generate e : gcd(e, (q-1)(p-1)(p+1)(q+1)) = 1
        BigInteger e = generateCoprime(p.subtract(BigInteger.ONE)
                .multiply(q.subtract(BigInteger.ONE)).
                multiply(p.add(BigInteger.ONE)).
                multiply(q.add(BigInteger.ONE)));
        Key key = new Key(e, N, true);
        //return key.toString().getBytes();
        return publicKey.getEncoded();
    }
    public byte[] generatePrivateKey(byte[] C)
    {
        BigInteger msg = new BigInteger(C);
        int lgdP = legandre(msg.pow(2).subtract(BigInteger.valueOf(4)), p);
        int lgdQ = legandre(msg.pow(2).subtract(BigInteger.valueOf(4)), q);
        BigInteger d = null;
        if(lgdP == -1 && lgdQ == -1)
            d = lcm(p.add(BigInteger.ONE), q.add(BigInteger.ONE));
        else if(lgdP == 1 && lgdQ == -1)
            d = lcm(p.subtract(BigInteger.ONE), q.add(BigInteger.ONE));
        else if(lgdP == -1 && lgdQ == 1)
            d = lcm(p.add(BigInteger.ONE), q.subtract(BigInteger.ONE));
        else if(lgdP == 1 && lgdQ == 1)
            d = lcm(p.subtract(BigInteger.ONE), q.subtract(BigInteger.ONE));

        //return (d.toString() + ":" + p.multiply(q).toString()).getBytes();
        return getbytes(new Key(d, p.multiply(q), false), false);

    }
    private BigInteger getBigIntFromKey(String key)
    {
        return BigInteger.ONE;
    }
    private byte[] _encrypt(byte[] message, byte[] key)
    {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(key);
            PublicKey pKey = keyFactory.generatePublic(publicKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pKey);
            return cipher.doFinal(message);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException
                | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }
    private byte[] _decrypt(byte[] message, byte[] key)
    {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            //EncodedKeySpec privateKeySpec = new X509EncodedKeySpec(key);
            //keyFactory.generatePrivate(privateKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(message);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }
    private byte[] _lucSequence(byte[] message, byte[] keys)
    {
        return _encrypt(message, keys);
    }
    private void init()
    {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
    public static List<BigInteger> extended_gcd(BigInteger a, BigInteger b)
    {
        BigInteger newX = new BigInteger("0");
        BigInteger tmpX = new BigInteger("0");
        BigInteger newY = new BigInteger("1");
        BigInteger tmpY = new BigInteger("1");
        BigInteger oldX = new BigInteger("1");
        BigInteger oldY = new BigInteger("0");
        BigInteger q = new BigInteger("0");
        BigInteger tmp;
        List<BigInteger> result;
       while (!b.equals(BigInteger.ZERO))
       {
           tmp = b;
           b = a.mod(tmp);
           q = a.divide(tmp);
           a = tmp;
           tmpX = newX;
           tmpY = newY;
           newX = oldX.subtract(newX.multiply(q));
           newY = oldY.subtract(newY.multiply(q));
           oldX = tmpX;
           oldY = tmpY;
       }
       result = List.of(a, oldX, oldY);
        return result;
    }

    /**
     *
     * @param P - luc sequence argument
     * @param N - module
     * @param n - number of sequence step
     * @return C - cypher code
     */
    private static BigInteger lucSequence(BigInteger P, BigInteger N, BigInteger n)
    {
        BigInteger _n = BigInteger.valueOf(1);
        BigInteger curValue = P;
        BigInteger counter = BigInteger.ZERO;
        if(lucValues.isEmpty())
        {
            lucValues.add(BigInteger.TWO);
            lucValues.add(P);
            while(_n.shiftLeft(1).compareTo(n) <= 0)
            {
                counter = counter.add(BigInteger.ONE);
                curValue = curValue.pow(2).mod(N).subtract(BigInteger.TWO);
                lucValues.add(curValue);
                _n = _n.shiftLeft(1);
            }
        }
        else
        {
            curValue = lucValues.get(n.bitLength() - 1);
//            System.out.println("bit Len: " + n.bitLength());
            _n = BigInteger.TWO.pow(n.bitLength() - 1);
        }
        BigInteger m = n.subtract(_n);
//        System.out.println("m: " + m);
        if(m.equals(BigInteger.ZERO))
            return curValue;
        return lucSequence(P, N, m).multiply(curValue).mod(N).subtract(lucSequence(P, N, _n.subtract(m)));

    }
    private static BigInteger lucRecursive(BigInteger P, BigInteger N, BigInteger n)
    {
        if(dec)
        {
            System.out.println("debCount: " + debCount + " n: " + n);
            debCount = debCount.add(BigInteger.ONE);
        }
        if(debCount.equals(BigInteger.valueOf(8634)))
            System.out.println("here");
        if(n.equals(BigInteger.ZERO))
            return BigInteger.TWO;
        if(n.equals(BigInteger.ONE))
            return P;

        // if n divide by two
        if (!n.testBit(0))
        {
            return lucRecursive(P, N, n.shiftRight(1)).mod(N).pow(2).subtract(BigInteger.TWO).mod(N);
        }
        else
        {
            BigInteger tmp = n.divide(BigInteger.TWO);
            return lucRecursive(P, N, tmp)
                    .multiply(lucRecursive(P, N, tmp.add(BigInteger.ONE)))
                    .subtract(P)
                    .mod(N);
        }
    }

    private byte[] getbytes(Key key, boolean isPublic)
    {
        if(isPublic)
            return publicKey.getEncoded();
        else
            return privateKey.getEncoded();
    }

    BigInteger pow(BigInteger base, BigInteger exponent, BigInteger mod) {
        BigInteger result = BigInteger.ONE;
        while (exponent.signum() > 0) {
            if (exponent.testBit(0)) result = result.multiply(base).mod(mod);
            base = base.multiply(base).mod(mod);
            exponent = exponent.shiftRight(1);
        }
        return result;
    }
    public BigInteger genPrime(int bitLen)
    {
        BigInteger num = new BigInteger(bitLen, new Random());
        // проверяем на четность
        if(!num.testBit(0))
            num = num.add(BigInteger.ONE);
        // идем от него и проверяеи на простоту
        while(!MillerRabinTest(num, 50))
        {
            System.out.println("num is not prime: " + num);
            num = num.add(BigInteger.TWO);
        }
        return num;
    }

    public boolean MillerRabinTest(BigInteger n, int k)
    {
        // если n == 2 или n == 3 - эти числа простые, возвращаем true
        if (n.equals(BigInteger.TWO) || n.equals(BigInteger.valueOf(3)))
            return true;
        // если n < 2 или n четное - возвращаем false
        if (n.compareTo(BigInteger.TWO) < 0 || !n.testBit(0))
            return false;
        // представим n − 1 в виде (2^s)·t, где t нечётно, это можно сделать последовательным делением n - 1 на 2
        BigInteger t = n.subtract(BigInteger.ONE);
        int s = 0;
        while (!t.testBit(0))
        {
            t = t.divide(BigInteger.TWO);
            s += 1;
        }
        // повторить k раз
        for (int i = 0; i < k; i++)
        {
            // выберем случайное целое число a в отрезке [2, n − 2]
            int nLen = n.bitLength();
            BigInteger a = new BigInteger(nLen - 2, new Random());
            // x ← a^t mod n, вычислим с помощью возведения в степень по модулю
            BigInteger x = pow(a, t, n);
            // если x == 1 или x == n − 1, то перейти на следующую итерацию цикла
            if (x.equals(BigInteger.ONE) || x.equals(n.subtract(BigInteger.ONE)))
                continue;
            // повторить s − 1 раз
            for (int r = 1; r < s; r++)
            {
                // x ← x^2 mod n
                x = pow(x, BigInteger.TWO, n);
                // если x == 1, то вернуть "составное"
                if (x.equals(BigInteger.ONE))
                    return false;
                // если x == n − 1, то перейти на следующую итерацию внешнего цикла
                if (x.equals(n.subtract(BigInteger.ONE)))
                    break;
            }
            if (!x.equals(n.subtract(BigInteger.ONE)))
                return false;
        }
        // вернуть "вероятно простое"
        return true;
    }

    private BigInteger lcm(BigInteger a, BigInteger b)
    {
        BigInteger gcd = a.gcd(b);
        return a.multiply(b).divide(gcd);
    }
//    public static BigInteger lucRecursive(BigInteger P)
    private static BigInteger debCount = BigInteger.ZERO;
    public static class Key
    {
        public Key(BigInteger ed, BigInteger N, boolean isPublic)
        {
            this.ed = ed;
            this.N = N;
            this.isPublic = isPublic;

        }
        public String toString()
        {
            return ed.toString() + ":" + N.toString();
        }
        @Setter @Getter
        private BigInteger ed;
        @Setter @Getter
        private BigInteger N;
        private boolean isPublic;
    }
}

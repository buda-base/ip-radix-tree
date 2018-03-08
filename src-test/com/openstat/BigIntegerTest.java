package com.openstat;

import org.testng.annotations.Test;

import java.math.BigInteger;
import java.net.InetAddress;

/**
 * Created by yangjifei on 20180208.
 * Update date:
 * <p>
 * Time: 1:19 PM
 * Project: ip-radix-tree
 * Package: com.openstat
 * Describe :
 * <p>
 * Result of Test: test ok,test error
 * Command:
 * <p>
 * <p>
 * Email:  jifei.yang@ngaa.com.cn
 * Status：Using online
 * Attention：
 */
public class BigIntegerTest {

    @Test
    public void testShiftLeftLong(){
        BigInteger temp=new BigInteger("12").shiftLeft(127).subtract(new BigInteger("1"));
        BigInteger temp02=new BigInteger("12").shiftRight(1).subtract(new BigInteger("1"));

        System.out.println(temp);
        System.out.println(temp02);
        long mask=1L<< 3;
        long mask02=1<< 3;

        long ri=3>>1;
        long ri02=3>>1;

        System.out.println(ri);
        System.out.println(ri02);

        System.out.println(mask);
        System.out.println(mask02);
    }

    @Test
    public void testLong(){
        long min=0x80000000L;
        long max=0xffffffffL;
        System.out.println("binary start  is "+min);
        System.out.println("binary end    is "+max);
    }

    @Test
    public void testShiftLeft(){
        BigInteger origin=new BigInteger("170141183460469248004086806286228826028");
        BigInteger bigInteger = origin.shiftRight(2);
        System.out.println(bigInteger);
    }

    @Test
    public void testIp2BigInteger(){
//        String ipv4="127.0.0.0";
        String ipv6="3e:0:0:0:2:0:0:e";
//        String ipv6="4001:DA8:0200:0:0:0:0:e34";
        try{
            BigInteger bigInteger = ipToBigInteger(ipv6);
            System.out.println("value is "+bigInteger);
        }catch (Exception exp){
            exp.printStackTrace();
        }
    }

    public  BigInteger ipToBigInteger(String addr) throws Exception{
        InetAddress a = InetAddress.getByName(addr);
        byte[] bytes = a.getAddress();
        return new BigInteger(1, bytes);
    }
}

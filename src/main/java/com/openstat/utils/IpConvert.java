package com.openstat.utils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by yangjifei on 20180208.
 * Update date:
 * <p>
 * Time: 4:35 PM
 * Project: ip-radix-tree
 * Package: com.openstat.utils
 * Describe :Ip convert utils .
 * <p>
 * Result of Test: test ok
 * Command:
 * <p>
 * <p>
 * Email:  jifei.yang@ngaa.com.cn
 * Status：Using online
 * Attention：
 */
public class IpConvert {

    /**
     * Prevent from being instantiated.
     */
    private IpConvert() {
    }

    /**
     * Converts the string ip address to BigInteger.
     *
     * @param ipInString
     *            String format ip address::https://www.ipaddressguide.com/ipv6-to-decimal
     * @return BigInteger
     */
    public static BigInteger stringToBigInt(String ipInString) {
        ipInString = ipInString.replace(" ", "");
        byte[] bytes;
        if (ipInString.contains(":"))
            bytes = ipv6ToBytes(ipInString);
        else
            bytes = ipv4ToBytes(ipInString);
        return new BigInteger(bytes);
    }

    /**
     * Converts the integer ip address to a string.
     *
     * @param ipInBigInt
     *            BigInteger ip
     * @return string ip
     */
    public static String bigIntToString(BigInteger ipInBigInt) {
        byte[] bytes = ipInBigInt.toByteArray();
        byte[] unsignedBytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        if (bytes.length == 4 || bytes.length == 16) {
            unsignedBytes = bytes;
        }
        // Remove sign bit
        try {
            String ip = InetAddress.getByAddress(unsignedBytes).toString();
            return ip.substring(ip.indexOf('/') + 1).trim();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ipv6 address turn to symbol byte[17]
     */
    private static byte[] ipv6ToBytes(String ipv6) {
        byte[] ret = new byte[17];
        ret[0] = 0;
        int ib = 16;
        boolean comFlag = false;   // ipv4 mixed mode flag
        if (ipv6.startsWith(":"))  // Remove the opening colon
            ipv6 = ipv6.substring(1);
        String groups[] = ipv6.split(":");
        for (int ig = groups.length - 1; ig > -1; ig--) {// Reverse scan
            if (groups[ig].contains(".")) {
                // Ipv4 mixed mode appears
                byte[] temp = ipv4ToBytes(groups[ig]);
                ret[ib--] = temp[4];
                ret[ib--] = temp[3];
                ret[ib--] = temp[2];
                ret[ib--] = temp[1];
                comFlag = true;
            } else if ("".equals(groups[ig])) {
                // Zero-length compression occurs and the number of missing groups is calculated
                int zlg = 9 - (groups.length + (comFlag ? 1 : 0));
                while (zlg-- > 0) {// Set these groups to 0
                    ret[ib--] = 0;
                    ret[ib--] = 0;
                }
            } else {
                int temp = Integer.parseInt(groups[ig], 16);
                ret[ib--] = (byte) temp;
                ret[ib--] = (byte) (temp >> 8);
            }
        }
        return ret;
    }

    /**
     * ipv4  address change to byte[5] symbol
     */
    private static byte[] ipv4ToBytes(String ipv4) {
        byte[] ret = new byte[5];
        ret[0] = 0;
        // First find the location of the midpoint of the IP address string
        int position1 = ipv4.indexOf(".");
        int position2 = ipv4.indexOf(".", position1 + 1);
        int position3 = ipv4.indexOf(".", position2 + 1);
        // Convert a string between each point to an integer
        ret[1] = (byte) Integer.parseInt(ipv4.substring(0, position1));
        ret[2] = (byte) Integer.parseInt(ipv4.substring(position1 + 1,
                position2));
        ret[3] = (byte) Integer.parseInt(ipv4.substring(position2 + 1,
                position3));
        ret[4] = (byte) Integer.parseInt(ipv4.substring(position3 + 1));
        return ret;
    }


    /**
     * Ipv6 to BigInteger
     * @param address ipv6
     * @return BigInteger
     * @throws UnknownHostException
     */
    @Deprecated
    public static BigInteger ipv6ToBigInteger(String address) throws UnknownHostException{
        InetAddress a = InetAddress.getByName(address.trim());
        byte[] bytes = a.getAddress();
        return new BigInteger(1, bytes);
    }

    /**
     * Ipv4 to long .
     * @param ipStr ipv4 str
     * @return Long
     * @throws UnknownHostException
     */
    public static long ipv4ToLong(String ipStr) throws UnknownHostException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putInt(0);
        bb.put(InetAddress.getByName(ipStr).getAddress());
        bb.rewind();
        return bb.getLong();
    }

}

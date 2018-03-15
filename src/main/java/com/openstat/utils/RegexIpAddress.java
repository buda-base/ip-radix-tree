package com.openstat.utils;

import sun.net.util.IPAddressUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by root on 20180207.
 * Update date:
 * <p>
 * Time: 2:29 PM
 * Project: ip-radix-tree
 * Package: com.openstat.utils
 * Describe :Regex utils .
 * <p>
 * Result of Test: test ok
 * Command:
 * <p>
 * <p>
 * Email:  jifei.yang@ngaa.com.cn
 * Status：Using online
 * Attention：
 */
public class RegexIpAddress {

    /**
     * Compile regex
     *
     * @param regex regex
     * @param cha   String
     * @return true or false
     */
    private static boolean matcherRegex(String regex,String cha){
        Pattern pattern= Pattern.compile(regex);
        Matcher matcher=pattern.matcher(cha);
        return matcher.matches();
    }

    /**
     * Judge the ip is legal or not.
     * 2018-3-15 10:34:10 This method has been abandoned, please do not use!
     * @param ip ip
     * @return result
     */
    @Deprecated
    public static Boolean isLegalIp(String ip){
        String  regexIpv4 = "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$";
        String  regexIpv6 = "^([\\da-fA-F]{1,4}:){7}[\\da-fA-F]{1,4}$";
        Boolean result=false;
        if(matcherRegex(regexIpv4,ip)||matcherRegex(regexIpv6,ip)){
            result=true;
        }
           return result;
    }

    /**
     * 2018-1-16 14:45:04
     * Content :Check the clientIp is ipv4 or ipv6 .
     * @param ip ip
     * @return  ipv4=4 ,ipv6=6 , other=0
     */
    public static int isIpv4OrIpv6(String ip){
        int result;
        if(IPAddressUtil.isIPv4LiteralAddress(ip)){
            result=4;
        }else if(IPAddressUtil.isIPv6LiteralAddress(ip)){
            result=6;
        }else{
            result=0;
        }
        return result;
    }
}

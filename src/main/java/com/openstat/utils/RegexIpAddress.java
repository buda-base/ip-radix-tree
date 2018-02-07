package com.openstat.utils;

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
     * @param ip ip
     * @return result
     */
    public static Boolean isLegalIp(String ip){
        String  regexIpv4 = "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$";
        String  regexIpv6 = "^([\\da-fA-F]{1,4}:){7}[\\da-fA-F]{1,4}$";
        Boolean result=false;
        if(matcherRegex(regexIpv4,ip)||matcherRegex(regexIpv6,ip)){
            result=true;
        }
           return result;
    }
}

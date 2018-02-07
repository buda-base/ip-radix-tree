/*
 * Copyright (C) 2012 Openstat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.openstat;


import com.openstat.utils.IdGen;
import com.openstat.utils.IpAddressMatcher;
import com.openstat.utils.RegexIpAddress;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;


/**
 * Created by root on 20180124.
 * Update date:
 * <p>
 * Time: 11:11 AM
 * Project: ip-radix-tree
 * Package: com.openstat
 * Describe :
 * <p>
 * Result of Test: test ok
 * Command:
 * <p>
 * <p>
 * Email:  jifei.yang@ngaa.com.cn
 * Status：Using online
 * Attention：
 */
public class SelectAreaTest {

    /**
     * 2018-02-08 Test ip is legal or not.
     */
    @Test
    public void judgeIpLegalOrNot(){
        String []ipv4={"1.1.1.12","251.2.1.1","256.2.3.45","25.2.3.45.23"};
        String []ipv6={"4001:DA8:0200:0:0:0:0:e34","40012:DA8:0200:0:0:0:0:e34","4001:DA8:0200:0:0:0:0:e34t","4001:DA8:0200:0:0:0:0:e34"};
        System.out.println("-----------------------------------ipv4------------------------------");
        for(String ip4:ipv4){
            Boolean res01=RegexIpAddress.isLegalIp(ip4);
            System.out.println(res01);
        }

        System.out.println("-----------------------------------ipv6------------------------------");
        for(String ip6:ipv6){
            Boolean res02=RegexIpAddress.isLegalIp(ip6);
            System.out.println(res02);
        }
    }

    /**
     * "2018-1-24 11:48:23 Test load local region file ipv4"
     */
    @Test
    public void selectIpV4() {
        String regionPath = "data/ipv4/ipv4-region.txt";
        try {
            IPv4RadixIntTree ipv4RadixIntTree = IPv4RadixIntTree.loadFromLocalFile(regionPath);

            BufferedReader br = new BufferedReader(new FileReader("data/ipv4/ipv4-list.txt"));
            String ipv4;
            int okCount = 0;
            int errorCount = 0;
            while ((ipv4 = br.readLine()) != null) {
                long lo = ipv4RadixIntTree.selectValue(ipv4);
                if (lo > 0) {
                    System.out.println(ipv4 + "  belongs to this section  ,and long value is [" + lo + "] .");
                    okCount++;
                } else {
                    errorCount++;
//                    System.out.println(ipv4+" do not  belongs to this section,and long value is ["+lo+"] .");
                }
            }
            System.out.println("The number of ok ip is " + okCount);
            System.out.println("The number of error ip is " + errorCount);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Test
    public void testIpv6Address() {
        String ipv6 = "2001:DA8:0200:0:0:0:0:e34";

        try {

            InetAddress byName = Inet6Address.getByName(ipv6);
            byte[] address = byName.getAddress();
            System.out.println(address);
        } catch (Exception exp) {
            exp.printStackTrace();
        }

    }

    @Test
    public void testIpv4Address() {
        String ipv4 = "127.0.0.5";
        try {
            byte[] address = InetAddress.getByName(ipv4).getAddress();
            System.out.println(new String(address));
        } catch (Exception exp) {
            exp.printStackTrace();
        }

    }

    /**
     * Check the ip is ipv6 or not.
     * @throws Exception
     */
    @Test
    public void ipv6RangeMatches() throws Exception {
        IpAddressMatcher matcher = new IpAddressMatcher("2001:DB8::/48");

        System.out.println( matcher.matches("2001:DB8:0:0:0:0:0:0"));
        System.out.println( matcher.matches("2001:DB8:0:0:0:0:0:1"));
        System.out.println( matcher.matches("2001:DB8:0:FFFF:FFFF:FFFF:FFFF:FFFF"));
        System.out.println( matcher.matches("2001:DB8:1:0:0:0:0:0"));
    }

    /**
     * Test get the world's id of generate 18 .
     */
    @Test
    public void testGetIdGen(){
        Long nextId=IdGen.get().nextId();
        System.out.println("Get the world's id is "+nextId);
        System.out.println("The length of id is "+nextId.toString().length());
        System.out.println(1 >>> 63 );

    }
}
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigInteger;


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
public class SelectIpv6AreaTest {
    private static Logger logger= LoggerFactory.getLogger(SelectIpv6AreaTest.class);

    /**
     * "2018-02-08 16:48:33 Test load local region file ipv6"
     * https://www.ultratools.com/tools/ipv6CIDRToRange
     */
    @Test
    public void selectIpV6() {
        String regionPath = "data/ipv6/ipv6-region.txt";
//        String regionPath = "data/ipv4/ipv4-region.txt";
        try {
            IPv6RadixBigIntegerTree ipv6RadixIntTree = IPv6RadixBigIntegerTree.loadFromLocalFile(regionPath);

            BufferedReader br = new BufferedReader(new FileReader("data/ipv6/ipv6-list.txt"));
//            BufferedReader br = new BufferedReader(new FileReader("data/ipv4/ipv4-list.txt"));
            String ipv6;
            int okCount = 0;
            int errorCount = 0;
            while ((ipv6 = br.readLine()) != null) {
                BigInteger lo = ipv6RadixIntTree.selectValue(ipv6);
                if (lo.longValue()>0) {
                    logger.warn(ipv6 + " ------>> belongs to this section  ,and long value is [" + lo.longValue() + "] .");
                    okCount++;
                } else {
                    errorCount++;
                    logger.warn(ipv6+" do not  belongs to this section,and long value is ["+lo.longValue() +"] .");
                }
            }
            logger.warn("The number of ok ip is " + okCount);
            logger.warn("The number of error ip is " + errorCount);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * 2018-3-7 14:47:04
     * Test BigInteger calculation.
     */
    @Test
    public void testBigInteger(){
        BigInteger start=new BigInteger("1");
        start=start.shiftLeft(127);
        // 10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
        System.out.println("start is "+start);

        BigInteger end=new BigInteger("1");
        end=end.shiftLeft(128);
        // 11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111
        System.out.println("end is "+end.subtract(new BigInteger("-1")));

    }


}
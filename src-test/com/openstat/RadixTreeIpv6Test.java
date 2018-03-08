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

import com.openstat.utils.IpConvert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;

import static org.testng.Assert.assertEquals;


public class RadixTreeIpv6Test {
    /**
     * Refer to https://www.ultratools.com/tools/ipv6CIDRToRange
     * we can calculation the cidr and the number of host.
     */
    @Test
    public void testCidrInclusion()throws Exception {
        IPv6RadixBigIntegerTree tr = new IPv6RadixBigIntegerTree(128);
        // aa:0:10:0:0:0:10:0/126
        tr.put("aa:0:10:0:0:0:10:0/126", new BigInteger("12345"));

        // bbbb:0:0:0:0:0:10:0/127
        tr.put("bbbb:0:0:0:0:0:10:0/127", new BigInteger("12346"));

        // cc:0:10:10:0:0:10:0/128
        tr.put("cc:0:10:10:0:0:10:0/128", new BigInteger("12347"));

        assertEquals(tr.selectValue(IpConvert.stringToBigInt("aa:0:10:0:0:0:10:0")), new BigInteger("12345"));

        assertEquals(tr.selectValue(IpConvert.stringToBigInt("aa:0:10:0:0:0:10:2")), new BigInteger("12345"));
        assertEquals(tr.selectValue(IpConvert.stringToBigInt("aa:0:10:0:0:0:10:3")), new BigInteger("12345"));

        assertEquals(tr.selectValue(IpConvert.stringToBigInt("bbbb:0:0:0:0:0:10:0")), new BigInteger("12346"));
        assertEquals(tr.selectValue(IpConvert.stringToBigInt("cc:0:10:10:0:0:10:0")), new BigInteger("12347"));
        assertEquals(tr.selectValue(IpConvert.stringToBigInt("bbbb:bbb:0:0:0:0:10:0")), IPv6RadixBigIntegerTree.NO_VALUE);
    }


}

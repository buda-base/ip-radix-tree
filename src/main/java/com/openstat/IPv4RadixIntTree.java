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

import com.openstat.utils.RegexIpAddress;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * A minimalistic, memory size-savvy and fairly fast radix tree (AKA Patricia trie)
 * implementation that uses IPv4 addresses with netmasks as keys and 32-bit signed
 * integers as values.
 * <p>
 * This tree is generally uses in read-only manner: there are no key removal operation
 * and the whole thing works best in pre-allocated fashion.
 * <p>
 * Update by highfei2011 in 2018-01-28 .
 */
public class IPv4RadixIntTree {
    /**
     * Special value that designates that there are no value stored in the key so far.
     * One can't use store value in a tree.
     */
    public static final int NO_VALUE = -1;

    private static final int NULL_PTR = -1;
    private static final int ROOT_PTR = 0;

    // 10000000000000000000000000000000 --> 32 bit
    private static final long MAX_IPV4_BIT = 0x80000000L;

    private int[] rights;
    private int[] lefts;
    private long[] values;
    private int allocatedSize;
    private int size;

    private static FileSystem fs = null;
    static {
        try {
            fs = FileSystem.get(new Configuration());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes IPv4 radix tree with default capacity of 1024 nodes. It should be sufficient for small databases.
     */
    public IPv4RadixIntTree() {
        init(1024);
    }

    /**
     * Initializes IPv4 radix tree with a given capacity.
     *
     * @param allocatedSize initial capacity to allocate
     */
    public IPv4RadixIntTree(int allocatedSize) {
        init(allocatedSize);
    }

    /**
     * Convert string ip to long.
     *
     * @param ipStr ip
     * @return long value
     * @throws UnknownHostException
     */
    private static long inetNtoa(String ipStr) throws UnknownHostException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putInt(0);
        bb.put(InetAddress.getByName(ipStr).getAddress());
        bb.rewind();
        return bb.getLong();
    }

    /**
     * The total number of count local  text file  lines.
     *
     * @param filename file name
     * @return The number of line .
     * @throws IOException
     */
    private static int countLinesInLocalFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename), 8192);
        int i = 0;
        while (br.readLine() != null) {
            i++;
        }
        return i;
    }

    /**
     * The total number of count hadoop distribute file system    text file  lines.
     *
     * @param filename file name
     * @return The number of line .
     * @throws IOException
     */
    private static int countLinesInHdfds(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(new Path(filename)), "utf-8"));
        int j = 0;
        while (br.readLine() != null) {
            j++;
        }
        return j;
    }

    /**
     * Initialize the size of the tree allocation.
     * @param allocatedSize  size
     */
    private void init(int allocatedSize) {
        this.allocatedSize = allocatedSize;

        rights = new int[this.allocatedSize];
        lefts = new int[this.allocatedSize];
        values = new long[this.allocatedSize];

        size = 1;
        lefts[0] = NULL_PTR;
        rights[0] = NULL_PTR;
        values[0] = NO_VALUE;
    }

    /**
     * Puts a key-value pair in a tree.
     *
     * @param key   IPv4 network prefix
     * @param mask  IPv4 netmask in networked byte order format (for example,
     *              0xffffff00L = 4294967040L corresponds to 255.255.255.0 AKA /24 network
     *              bitmask)
     * @param value an arbitrary value that would be stored under a given key
     */
    public void put(long key, long mask, long value) {
        long bit = MAX_IPV4_BIT;
        int node = ROOT_PTR;
        int next = ROOT_PTR;

        while ((bit & mask) != 0) {
            next = ((key & bit) != 0) ? rights[node] : lefts[node];
            if (next == NULL_PTR)
                break;
            bit >>= 1;
            node = next;
        }

        if (next != NULL_PTR) {
            values[node] = value;
            return;
        }

        while ((bit & mask) != 0) {
            if (size == allocatedSize) {
                expandAllocatedSize();
            }
            next = size;
            values[next] = NO_VALUE;
            rights[next] = NULL_PTR;
            lefts[next] = NULL_PTR;

            if ((key & bit) != 0) {
                rights[node] = next;
            } else {
                lefts[node] = next;
            }

            bit >>= 1;
            node = next;
            size++;
        }

        values[node] = value;
    }

    /**
     * Expand Allocated Size
     */
    private void expandAllocatedSize() {
        int oldSize = allocatedSize;
        allocatedSize *= 2;

        int[] newLefts = new int[allocatedSize];
        System.arraycopy(lefts, 0, newLefts, 0, oldSize);
        lefts = newLefts;

        int[] newRights = new int[allocatedSize];
        System.arraycopy(rights, 0, newRights, 0, oldSize);
        rights = newRights;

        long[] newValues = new long[allocatedSize];
        System.arraycopy(values, 0, newValues, 0, oldSize);
        values = newValues;
    }

    /**
     * Selects a value for a given IPv4 address, traversing tree and choosing
     * most specific value available for a given address.
     *
     * @param key IPv4 address to look up
     * @return value at most specific IPv4 network in a tree for a given IPv4
     * address
     */
    public long selectValue(long key) {
        long bit = MAX_IPV4_BIT;
        long value = NO_VALUE;
        int node = ROOT_PTR;

        while (node != NULL_PTR) {
            if (values[node] != NO_VALUE) {
                value = values[node];
            }
            node = ((key & bit) != 0) ? rights[node] : lefts[node];
            bit >>= 1;
        }

        return value;
    }

    /**
     * Puts a key-value pair in a tree, using a string representation of IPv4 prefix.
     *
     * @param ipNet IPv4 network as a string in form of "a.b.c.d/e", where a, b, c, d
     *              are IPv4 octets (in decimal) and "e" is a netmask in CIDR notation
     * @param value an arbitrary value that would be stored under a given key
     * @throws UnknownHostException
     */
    public void put(String ipNet, long value) throws UnknownHostException {
        int pos = ipNet.indexOf('/');
        String ipStr = ipNet.substring(0, pos);
        long ip = inetNtoa(ipStr);

        String netMaskStr = ipNet.substring(pos + 1);
        int cidr = 0;
        try {
            cidr = Integer.parseInt(netMaskStr.trim());
        } catch (NumberFormatException e) {
            System.out.println("error ::: " + netMaskStr);
        }

        long netMask = ((1L << (32 - cidr)) - 1L) ^ 0xffffffffL;
        put(ip, netMask, value);
    }

    /**
     * Selects a value for a given IPv4 address, traversing tree and choosing
     * most specific value available for a given address.
     *
     * @param ipStr IPv4 address to look up, in string form (i.e. "a.b.c.d")
     * @return value at most specific IPv4 network in a tree for a given IPv4
     * address
     * @throws UnknownHostException
     */
    public long selectValue(String ipStr) throws UnknownHostException {
        return selectValue(inetNtoa(ipStr));
    }

    /**
     * Returns a size of tree in number of nodes (not number of prefixes stored).
     *
     * @return a number of nodes in current tree
     */
    public int size() {
        return size;
    }


    /**
     * Helper function that reads IPv4 radix tree from a local file in tab-separated format:
     * (IPv4 net => value)
     * Default format :not nginx
     *
     * @param filename name of a local file to read
     * @return a fully constructed IPv4 radix tree from that file
     * @throws IOException
     */
    public static IPv4RadixIntTree loadFromLocalFile(String filename) throws IOException {
        return loadFromLocalFile(filename, false);
    }

    /**
     * Helper function that reads IPv4 radix tree from a local file in tab-separated format:
     * (IPv4 net => value)
     * Default format :not nginx
     *
     * @param filename name of a local file to read
     * @return a fully constructed IPv4 radix tree from that file
     * @throws IOException
     */
    public static IPv4RadixIntTree loadFromHdfsFile(String filename) throws IOException {
        return loadFromHdfs(filename, false);
    }

    /**
     * Helper function that reads IPv4 radix tree from a local file in tab-separated format:
     * (IPv4 net => value)
     *
     * @param filename    name of a local file to read
     * @param nginxFormat if true, then file would be parsed as nginx web server configuration file:
     *                    "value" would be treated as hex and last symbol at EOL would be stripped (as normally nginx
     *                    config files has lines ending with ";")
     * @return a fully constructed IPv4 radix tree from that file
     * @throws IOException
     */
    public static IPv4RadixIntTree loadFromLocalFile(String filename, boolean nginxFormat) throws IOException {
        IPv4RadixIntTree tr = new IPv4RadixIntTree(countLinesInLocalFile(filename));
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String l;
        long value;
        /*
         line (cidr,nextId,ispId,regionId,regionlevel,regionType,networkType)
         112.60.0.0/18	951728549285331151	2	34	3	2	0
         */

        while ((l = br.readLine()) != null) {
            String[] c = l.split("\\s+", -1);

            if (nginxFormat) {
                // strip ";" at EOL
                c[1] = c[1].substring(0, c[1].length() - 1);

                // NB: This is to work around malicious "80000000" AS number
                value = Long.parseLong(c[1], 16);
            } else {
                // NB: You can adjust the use of int or long.
                value = Long.parseLong(c[1]);
            }

            ////////////////////////////////////////////////////////
            // Judge the text of the ip is legal!
            String ip=c[0].split("/")[0];
            if(RegexIpAddress.isLegalIp(ip)){
                tr.put(c[0].trim(), value);
            }

        }

        return tr;
    }

    /**
     * Read region file from hadoop distribute file system .
     *
     * @param filePath    filePath
     * @param nginxFormat format
     * @return IPv4RadixIntTree
     * @throws IOException
     */
    public static IPv4RadixIntTree loadFromHdfs(String filePath, boolean nginxFormat) throws IOException {
        IPv4RadixIntTree tr = new IPv4RadixIntTree(countLinesInHdfds(filePath));
        BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(new Path(filePath)), "UTF-8"));
        String l;
        long value;
        /*
         line (cidr,nextId,ispId,regionId,regionlevel,regionType,networkType)
         112.60.0.0/18	951728549285331151	2	34	3	2	0
         */

        while ((l = br.readLine()) != null) {
            String[] c = l.split("\t", -1);

            if (nginxFormat) {
                // strip ";" at EOL
                c[1] = c[1].substring(0, c[1].length() - 1);

                // NB: this is to work around malicious "80000000" AS number
                value = Long.parseLong(c[1], 16);
            } else {
                value = Long.parseLong(c[1]);
            }
            ////////////////////////////////////////////
            // Judge the text of the ip is legal or not!
            String ip=c[0].split("/")[0];
            if(RegexIpAddress.isLegalIp(ip)){
                tr.put(c[0].trim(), value);
            }

        }

        return tr;
    }

}

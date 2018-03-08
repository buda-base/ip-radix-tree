package com.openstat;

import com.openstat.utils.IpConvert;
import com.openstat.utils.RegexIpAddress;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.math.BigInteger;
import java.net.UnknownHostException;


/**
 * A minimalistic, memory size-savvy and fairly fast radix tree (AKA Patricia trie)
 * implementation that uses IPv6 addresses with netmasks as keys and 128-bit BigInteger as values.
 * <p>
 * This tree is generally uses in read-only manner: there are no key removal operation
 * and the whole thing works best in pre-allocated fashion.
 *
 * <p>
 * Result of Test: test ok
 * Update by highfei2011 in 2018-02-08 .
 */
public class IPv6RadixBigIntegerTree {
    private static Logger logger= LoggerFactory.getLogger(IPv6RadixBigIntegerTree.class);
    /**
     * Special value that designates that there are no value stored in the key so far.
     * One can't use store value in a tree.
     */
    public static final BigInteger NO_VALUE = new BigInteger("-1");
    private static final int NULL_PTR = -1;
    private static final int ROOT_PTR = 0;

    // Ipv6 start value
    // 10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
    private static final BigInteger IPV6_START_VALUE = new BigInteger("170141183460469231731687303715884105728");

    // Ipv6 end value
    // 11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111
    private static final BigInteger IPV6_END_VALUE   = new BigInteger("340282366920938463463374607431768211455");

    // zero value
    private static final BigInteger ZERO_VALUE=BigInteger.ZERO;

    // Split char
    private static final String LINE_SPLIT = "\\s+";

    // Nginx Length
    private static final int NGINX_LENGTH = 16;

    // cidr max
    private static final int CIDR_MAX_LENGTH = 128;

    // your hadoop dir
    private static final  String HADOOP_DIR="/opt/soft/hadoop-2.6.0";

    private int[] rights;
    private int[] lefts;
    private BigInteger[] values;
    private int allocatedSize;
    private int size;

    private static FileSystem fs = null;
    static {
        try {
            System.setProperty("hadoop.home.dir",HADOOP_DIR);
            fs = FileSystem.get(new Configuration());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes IPv4 radix tree with default capacity of 1024 nodes. It should be sufficient for small databases.
     */
    public IPv6RadixBigIntegerTree() {
        init(2048);
    }

    /**
     * Initializes IPv6 radix tree with a given capacity.
     *
     * @param allocatedSize initial capacity to allocate
     */
    public IPv6RadixBigIntegerTree(int allocatedSize) {
        init(allocatedSize);
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
        values = new BigInteger[this.allocatedSize];

        size = 1;
        lefts[0] = NULL_PTR;
        rights[0] = NULL_PTR;
        values[0] = NO_VALUE;
    }

    /**
     * Puts a key-value pair in a tree.
     *
     * @param key   IPv6 network prefix
     * @param mask  IPv6 netmask in networked byte order format
     * @param value An arbitrary value that would be stored under a given key
     */
    public void put(BigInteger key, BigInteger mask, BigInteger value) {
        BigInteger bit = IPV6_START_VALUE;
        int node = ROOT_PTR;
        int next = ROOT_PTR;

        while ( (bit.and(mask)).compareTo(ZERO_VALUE)!=0 ) {
            next = (key.and(bit)).compareTo(ZERO_VALUE)!=0 ? rights[node] : lefts[node];
            if (next == NULL_PTR)
                break;
            bit=bit.shiftRight(1);
            node = next;
        }

        if (next != NULL_PTR) {
            values[node] = value;
            return;
        }

        while ((bit.and(mask)).compareTo(ZERO_VALUE)!=0) {
            if (size == allocatedSize) {
                expandAllocatedSize();
            }
            next = size;
            values[next] = NO_VALUE;
            rights[next] = NULL_PTR;
            lefts[next] = NULL_PTR;

            if ((key.and(bit)).compareTo(ZERO_VALUE)!=0) {
                rights[node] = next;
            } else {
                lefts[node] = next;
            }

            bit=bit.shiftRight(1);
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

        BigInteger[] newValues = new BigInteger[allocatedSize];
        System.arraycopy(values, 0, newValues, 0, oldSize);
        values = newValues;
    }

    /**
     * Selects a value for a given IPv6 address, traversing tree and choosing
     * most specific value available for a given address.
     *
     * @param key IPv6 address to look up
     * @return value at most specific IPv6 network in a tree for a given IPv6
     * address
     */
    public BigInteger selectValue(BigInteger key) {
        logger.debug("Input the ip of select is {} .",key);
        BigInteger bit = IPV6_START_VALUE;
        BigInteger value = NO_VALUE;
        int node = ROOT_PTR;

        while (node != NULL_PTR && values[node]!=null) {
            if (values[node].compareTo(NO_VALUE)!=0 ) {
                value = values[node];
                logger.debug("The value of the query is:{} .",value);
            }

            logger.debug("key.and(bit)  is {}. ",key.and(bit));
            node = (key.and(bit)).compareTo(ZERO_VALUE)!=0  ? rights[node] : lefts[node];
            bit=bit.shiftRight(1);
            logger.debug("bit is {}",bit);
        }

        return value;
    }

    /**
     * Puts a key-value pair in a tree, using a string representation of IPv6 prefix.
     *
     * @param ipNet IPv6 network as a string in form of "a.b.c.d/e", where a, b, c, d
     *              are IPv6 octets (in decimal) and "e" is a netmask in CIDR notation
     * @param value an arbitrary value that would be stored under a given key
     * @throws UnknownHostException
     */
    public void put(String ipNet, BigInteger value) throws Exception {
        int pos = ipNet.indexOf('/');
        String ipStr = ipNet.substring(0, pos);

        BigInteger ip = IpConvert.stringToBigInt(ipStr);

        String netMaskStr = ipNet.substring(pos + 1);

        int cidr = 0;
        try {
            cidr = Integer.parseInt(netMaskStr.trim());
        } catch (NumberFormatException e) {
            logger.error("Parse the net mask occur a error: {}" , netMaskStr);
        }

        BigInteger temp=(new BigInteger("1").shiftLeft(CIDR_MAX_LENGTH - cidr)).subtract(new BigInteger("1"));

        BigInteger netMask = temp.xor(IPV6_END_VALUE);
        logger.debug("Put       ip           is "+ipNet+" and the bigInteger of ip  is  {}",ip);
        logger.debug("This      netMaskStr   is {} .",netMaskStr);
        logger.debug("This      new  netMask is {} .",netMask);
        put(ip, netMask, value);
    }

    /**
     * Selects a value for a given IPv6 address, traversing tree and choosing
     * most specific value available for a given address.
     *
     * @param ipStr IPv6 address to look up, in string form (i.e. "a.b.c.d")
     * @return value at most specific IPv6 network in a tree for a given IPv6
     * address
     * @throws Exception
     */
    public BigInteger selectValue(String ipStr) throws Exception {
        return selectValue(IpConvert.stringToBigInt(ipStr));
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
     * Helper function that reads IPv6 radix tree from a local file in tab-separated format:
     * (IPv6 net => value)
     * Default format :not nginx
     *
     * @param filename name of a local file to read
     * @return A fully constructed IPv4 radix tree from that file
     * @throws IOException
     */
    public static IPv6RadixBigIntegerTree loadFromLocalFile(String filename) throws Exception {
        return loadFromLocalFile(filename, false);
    }

    /**
     * Helper function that reads IPv6 radix tree from a local file in tab-separated format:
     * (IPv6 net => value)
     * Default format :not nginx
     *
     * @param filename name of a local file to read
     * @return A fully constructed IPv4 radix tree from that file
     * @throws IOException
     */
    public static IPv6RadixBigIntegerTree loadFromHdfsFile(String filename) throws Exception {
        return loadFromHdfs(filename, false);
    }

    /**
     * Helper function that reads IPv6 radix tree from a local file in tab-separated format:
     * (IPv6 net => value)
     *
     * @param filename    name of a local file to read
     * @param nginxFormat if true, then file would be parsed as nginx web server configuration file:
     *                    "value" would be treated as hex and last symbol at EOL would be stripped (as normally nginx
     *                    config files has lines ending with ";")
     * @return a fully constructed IPv6 radix tree from that file
     * @throws IOException
     */
    public static IPv6RadixBigIntegerTree loadFromLocalFile(String filename, boolean nginxFormat) throws Exception {
        IPv6RadixBigIntegerTree tr = new IPv6RadixBigIntegerTree(countLinesInLocalFile(filename));
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String l;
        BigInteger value;
        /*
         line (cidr,nextId,ispId,regionId,regionlevel,regionType,networkType)
         4501:DA8:0203:0:0:0:0:0/16	951728549285331151	2	34	3	2	0
         */

        while ((l = br.readLine()) != null) {
            String[] c = l.split(LINE_SPLIT, -1);

            if (nginxFormat) {
                // strip ";" at EOL
                int le=c[1].length() - 1;
                // Put value
                c[1] = c[1].substring(0,le);
                // NB: This is to work around malicious "80000000" AS number
                value = new BigInteger(c[1], NGINX_LENGTH);
            } else {
                // NB: You can adjust the use of int or long.
                value = new BigInteger(c[1]);
            }

            ////////////////////////////////////////////////////////
            // Judge the text of the ip is legal!
            String ip=c[0].split("/")[0];
            if(RegexIpAddress.isLegalIp(ip)){
                logger.debug("File      ip           is {}",c[0]);
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
     * @return IPv6RadixBigIntegerTree
     * @throws IOException
     */
    public static IPv6RadixBigIntegerTree loadFromHdfs(String filePath, boolean nginxFormat) throws Exception {
        IPv6RadixBigIntegerTree tr = new IPv6RadixBigIntegerTree(countLinesInHdfds(filePath));
        BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(new Path(filePath)), "UTF-8"));
        String l;
        BigInteger value;
        /*
         line (cidr,nextId,ispId,regionId,regionlevel,regionType,networkType)
         4501:DA8:0203:0:0:0:0:0/16	951728549285331151	2	34	3	2	0
         */
        while ((l = br.readLine()) != null) {
            String[] c = l.split(LINE_SPLIT, -1);

            if (nginxFormat) {
                // strip ";" at EOL
                c[1] = c[1].substring(0, c[1].length() - 1);

                // NB: this is to work around malicious "80000000" AS number
                value = new BigInteger(c[1], NGINX_LENGTH);
            } else {
                value = new BigInteger(c[1]);
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

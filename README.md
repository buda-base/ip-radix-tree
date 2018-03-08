This project provides a fast, robust and memory-savvy implementation
of IPv4 radix tree and IPv6 radix tree (AKA Patricia trie) in Java.

The API roughly follows Java Collections API and can be easily
demonstrated in a following snippet:

`IPv4 lookup`

```java
IPv4RadixIntTree tr = new IPv4RadixIntTree();
tr.put(0x0a000000, 0xffffff00, 42);
tr.put(0x0a000000, 0xff000000, 69);
tr.put("10.0.3.0/24", 123);
int v1 = tr.selectValue(0x0a202020); // => 69, as 10.32.32.32 belongs to 10.0.0.0/8
int v2 = tr.selectValue(0x0a000020); // => 42, as 10.0.0.32 belongs to 10.0.0.0/24
int v3 = tr.selectValue("10.0.3.5"); // => 123, as 10.0.3.5 belongs to 10.0.3.0/24
```

`IPv6 lookup`
```java
IPv6RadixBigIntegerTree tr = new IPv6RadixBigIntegerTree();
tr.put("aa:0:10:0:0:0:10:0/126", new BigInteger("12345"));
tr.put("bbbb:0:0:0:0:0:10:0/127", new BigInteger("12346"));
tr.put("cc:0:10:10:0:0:10:0/128", new BigInteger("12347"));

BigInteger v1=tr.selectValue(IpConvert.stringToBigInt("aa:0:10:0:0:0:10:0"))  // => new BigInteger("12345"))  belongs to aa:0:10:0:0:0:10:0/126
BigInteger v2=tr.selectValue(IpConvert.stringToBigInt("bbbb:0:0:0:0:0:10:0")) // => new BigInteger("12346")) belongs to bbbb:0:0:0:0:0:10:0/127
BigInteger v3=tr.selectValue(IpConvert.stringToBigInt("cc:0:10:10:0:0:10:0")) // => new BigInteger("12347")) belongs to cc:0:10:10:0:0:10:0/128

```
## Memory consumption ##

Memory consumption for this implementation is tuned to be the lowest
possible. No Java internal objects or object references are used, as
they consume too much memory per node (on 64-bit JVM, it's something
like 32 bytes per object).

We use pre-allocated simple Java int[] arrays, one for left branch
pointers, one for right branch pointers and one for values. It takes
exactly 3 * 4 = 12 bytes per node.

Number of nodes in a tree greatly depends on tree topology: a rough
estimate for generic IPv4 routing trees is something like from 3 up to
8 times the number of IP prefixes.

For example, a test case included with this distribution has 392415 IP
prefixes and it generates up to 946225 nodes in memory, thus consuming
about 10.8 megabytes of heap.

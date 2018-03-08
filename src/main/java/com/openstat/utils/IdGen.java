package com.openstat.utils;

/**
 * Created by root on 20180129.
 * Update date:
 * <p>
 * Time: 10:38 AM
 * Project: ip-radix-tree
 * Package: com.openstat.utils
 * Describe :Generate the world's only 18-digit Id number.
 * <p>       Original link -->http://www.blogjava.net/bolo/archive/2015/07/13/426200.html
 * Result of Test: test ok
 * Command:
 * <p>
 * <p>
 * Email:  jifei.yang@ngaa.com.cn
 * Status：Using online
 * Attention：
 */
public class IdGen {
    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long twepoch = 1288834974657L;                                            //  Thu, 04 Nov 2010 01:42:54 GMT.
    private long workerIdBits = 5L;                                                   //  The length of the node ID.
    private long datacenterIdBits = 5L;                                               //  The length of the data center ID.
    private long maxWorkerId = -1L ^ (-1L << workerIdBits);                           //  Maximum supported machine nodes 0~31, a total of 32.
    private long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);                   //  The maximum number of supported data center nodes is 0 to 31, for a total of 32.
    private long sequenceBits = 12L;                                                  //  12-digit serial number.
    private long workerIdShift = sequenceBits;                                        //  Machine node left 12-bit.
    private long datacenterIdShift = sequenceBits + workerIdBits;                     //  Data Center Node Left 17 Bits.
    private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits; //  Time milliseconds left shift 22 bits.
    private long sequenceMask = -1L ^ (-1L << sequenceBits);                          //  4095
    private long lastTimestamp = -1L;

    private static class IdGenHolder {
        private static final IdGen instance = new IdGen();
    }

    public static IdGen get(){
        return IdGenHolder.instance;
    }

    private IdGen() {
        this(0L, 0L);
    }

    private IdGen(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        // Get the current number of milliseconds.
        long timestamp = timeGen();
        // If there is a problem with the server time (clock back), an error occurs.
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        // If the previous generation time is the same as the current time, it is within the same millisecond.
        if (lastTimestamp == timestamp) {
            // Sequence since the increase, because the sequence only 12bit, so phaseMask and phase, remove the high.
            sequence = (sequence + 1) & sequenceMask;
            // Determine whether the overflow, that is, more than 4095 per millisecond, when 4096, with sequenceMask and phase sequence is equal to 0.
            if (sequence == 0) {
                // Spin waits until the next millisecond.
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // If and the last generation time is different, reset the sequence, is the beginning of the next millisecond, sequence count again starting from 0 cumulative.
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        // Finally spell ID according to the rules.
        // 000000000000000000000000000000000000000000  00000            00000       000000000000
        // time                                                               datacenterId   workerId    sequence
        return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift) | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}

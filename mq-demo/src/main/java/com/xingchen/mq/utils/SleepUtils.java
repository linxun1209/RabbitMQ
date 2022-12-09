package com.xingchen.mq.utils;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.utils
 * @date 2022/12/4 16:50
 */
public class SleepUtils {
    public static void sleep(int second) {
        try {
            Thread.sleep(1000 * second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.currentThread().interrupt();
    }
}

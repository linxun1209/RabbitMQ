package com.xingchen.mq.Client4;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.xingchen.mq.utils.RabbitMqUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Client4
 * @date 2022/12/5 20:51
 */
/*
 * 发布确认模式，
 * 1、单个确认
 * 2、批量确认
 * 3、异步批量确认
 * */
public class ComfirmMessage {

    // 批量发消息的个数
    public static final int MESSAGE_COUNT = 1000;

    public static void main(String[] args) throws Exception {
//        // 1、单个确认
//        // 发布1000个单独确认消息，耗时35325ms
//        ComfirmMessage.publishMessageIndividually();

//
////        2.批量确认
////        耗时116ms
//        ComfirmMessage.publishMessageBatch();


////         * 3、异步批量确认
//        发布1000个异步确认消息，耗时71ms
//        ComfirmMessage.publicMessageAsync();


//        处理异步未确认信息

        ComfirmMessage.publicMessageAsync2();


    }

    public static void publishMessageIndividually() throws Exception {
        Channel channel = RabbitMqUtils.getChannel();
        String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName, false, false, false, null);

        // 开启发布确认
        channel.confirmSelect();
        // 开始时间
        long begin = System.currentTimeMillis();

        // 批量发消息
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String message = i + "";
            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
            // 单个消息马上进行发布确认
            boolean flag = channel.waitForConfirms();
            if (flag) {

            }
        }

        // 结束时间
        long end = System.currentTimeMillis();
        System.out.println("发布" + MESSAGE_COUNT + "个单独确认消息，耗时" + (end - begin) + "ms");
    }


    public static void publishMessageBatch() throws Exception {
        Channel channel = RabbitMqUtils.getChannel();
        String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName, false, false, false, null);

        // 开启发布确认
        channel.confirmSelect();
        // 开始时间
        long begin = System.currentTimeMillis();

        // 批量确认消息大小
        int batchSize = 1000;

        // 批量发送 批量确认
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String message = i + "";
            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));

            // 判断达到100条消息的时候，批量确认一次
            if (i % batchSize == 0) {
                // 确认发布
                channel.waitForConfirms();
            }
        }

        // 结束时间
        long end = System.currentTimeMillis();
        System.out.println("发布" + MESSAGE_COUNT + "个批量确认消息，耗时" + (end - begin) + "ms");
    }

    public static void publicMessageAsync() throws Exception {
        Channel channel = RabbitMqUtils.getChannel();
        String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName, false, false, false, null);

        // 开启发布确认
        channel.confirmSelect();
        // 开始时间
        long begin = System.currentTimeMillis();

        // 消息确认成功回调函数
        ConfirmCallback ackCallback = (deliveryTag, multiply) -> {
            System.out.println("确认的消息：" + deliveryTag);
        };

        // 消息确认失败回调函数
        /*
         * 参数1：消息的标记
         * 参数2：是否为批量确认
         * */
        ConfirmCallback nackCallback = (deliveryTag, multiply) -> {
            System.out.println("未确认的消息：" + deliveryTag);
        };

        // 准备消息的监听器，监听哪些消息成功，哪些消息失败
        /*
         * 参数1：监听哪些消息成功
         * 参数2：监听哪些消息失败
         * */
        channel.addConfirmListener(ackCallback, nackCallback);

        // 批量发送消息
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String message = "消息" + i;
            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
        }

        // 结束时间
        long end = System.currentTimeMillis();
        System.out.println("发布" + MESSAGE_COUNT + "个异步确认消息，耗时" + (end - begin) + "ms");
    }


    public static void publicMessageAsync2() throws Exception {
        Channel channel = RabbitMqUtils.getChannel();
        String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName, false, false, false, null);

        // 开启发布确认
        channel.confirmSelect();

        /*
         * 线程安全有序的一个哈希表 适用于高并发的情况下
         * 1、轻松地将序号与消息进行关联
         * 2、轻松地批量删除，只要给到序号
         * 3、支持高并发
         * */
        ConcurrentSkipListMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();

        // 消息确认成功回调函数
        ConfirmCallback ackCallback = (deliveryTag, multiply) -> {
            // 删除到已经确认的消息，剩下的就是未确认的消息
            if (multiply) {
                ConcurrentNavigableMap<Long, String> confiremed = outstandingConfirms.headMap(deliveryTag);
                confiremed.clear();
            } else {
                outstandingConfirms.remove(deliveryTag);
            }

            System.out.println("确认的消息：" + deliveryTag);
        };

        // 消息确认失败回调函数
        /*
         * 参数1：消息的标记
         * 参数2：是否为批量确认
         * */
        ConfirmCallback nackCallback = (deliveryTag, multiply) -> {
            // 打印一下未确认的消息都有哪些
            String message = outstandingConfirms.get(deliveryTag);
            System.out.println("未确认的消息是：" + message + "未确认的消息tag：" + deliveryTag);
        };

        // 准备消息的监听器，监听哪些消息成功，哪些消息失败
        /*
         * 参数1：监听哪些消息成功
         * 参数2：监听哪些消息失败
         * */
        channel.addConfirmListener(ackCallback, nackCallback);

        // 开始时间
        long begin = System.currentTimeMillis();

        // 批量发送消息
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String message = "消息" + i;
            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));

            // 此处记录下所有要发送的消息的总和
            outstandingConfirms.put(channel.getNextPublishSeqNo(), message);
        }


        // 结束时间
        long end = System.currentTimeMillis();
        System.out.println("发布" + MESSAGE_COUNT + "个异步确认消息，耗时" + (end - begin) + "ms");
    }

}

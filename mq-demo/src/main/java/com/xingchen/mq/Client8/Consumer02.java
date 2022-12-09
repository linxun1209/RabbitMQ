package com.xingchen.mq.Client8;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.xingchen.mq.utils.RabbitMqUtils;


/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Client8
 * @date 2022/12/7 10:35
 */
public class Consumer02 {

    public static final String DEAD_QUEUE = "dead_queue";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMqUtils.getChannel();
        System.out.println("等待接收消息中.........");


        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println("Consumer02接收的消息是：" + new String(message.getBody()));
        };

        channel.basicConsume(DEAD_QUEUE, true, deliverCallback, consumerTag -> {
        });
    }
}


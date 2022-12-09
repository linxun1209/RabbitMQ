package com.xingchen.mq.Clinet2;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.xingchen.mq.utils.RabbitMqUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Clinet2
 * @date 2022/12/4 16:08
 */
public class Worker01 {

    // 队列名称
    public static final String QUEUE_NAME = "hello";

    // 接受消息
    public static void main(String[] args) throws IOException, TimeoutException {
        Channel channel = RabbitMqUtils.getChannel();

        // 接受消息参数
        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println("接受到的消息：" + message.getBody());
        };

        // 取消消费参数
        CancelCallback cancelCallback = consumerTag -> {
            System.out.println(consumerTag + "消费者取消消费借口回调逻辑");
        };
        System.out.println("c1等待接收消息");
        // 消息的接受
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, cancelCallback);
    }
}


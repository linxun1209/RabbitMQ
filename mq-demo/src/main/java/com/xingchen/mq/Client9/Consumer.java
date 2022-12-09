package com.xingchen.mq.Client9;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Client9
 * @date 2022/12/9 17:18
 */
public class Consumer {
    // 队列名称
    public static final String QUEUE_NAME = "hello";

    // 接受消息
    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("192.168.163.128");
        factory.setUsername("admin");
        factory.setPassword("123");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // 声明 接受消息
        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println(new String(message.getBody()));
        };
        // 声明 取消消息
        CancelCallback cancelCallback = consumer -> {
            System.out.println("消息消费被中断");
        };
        System.out.println("C2等待接受消息.......");
        channel.basicConsume(QUEUE_NAME,true,deliverCallback,cancelCallback);
    }
}

package com.xingchen.mq.Clinet1;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq
 * @date 2022/12/4 15:37
 */
public class Consumer {
    // 队列名称
    public static final String QUEUE_NAME = "hello";

    // 接受消息
    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("110.40.211.224");
        factory.setUsername("admin");
        factory.setPassword("123456");
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

        /*
         * 消费者接收消息
         * 参数1：表示消费哪个UI列
         * 参数2：消费成功之后，是否需要自动应答，true表示自动应答，false表示手动应答
         * 参数3：消费者成功消费的回调
         * 参数4：消费者取消消费的回调
         */
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, cancelCallback);
    }
}

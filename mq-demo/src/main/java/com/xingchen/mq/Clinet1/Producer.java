package com.xingchen.mq.Clinet1;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq
 * @date 2022/12/4 15:35
 */
public class Producer {
    // 队列名称
    public static final String QUEUE_NAME = "hello";

    // 发消息
    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();

        // 工厂IP连接RabbitMQ的队列
        factory.setHost("110.40.211.224");
        // 用户名
        factory.setUsername("admin");
        // 密码
        factory.setPassword("123456");

        factory.setPort(5672);

        // 创建连接
        Connection connection = factory.newConnection();
        // 获取信道
        Channel channel = connection.createChannel();
        /*
         * 生成一个队列
         * 参数1：队列名称
         * 参数2：队列里面的消息是否持久化，默认情况下，消息存储在内存中
         * 参数3：该队列是否只供一个消费者进行消费，是否进行消费共享，true可以多个消费者消费，
         *        false只能一个消费者消费
         * 参数4：是否自动删除：最后一个消费者断开连接之后，该队列是否自动删除，true则自动删除，
         *        false不自动删除
         * 参数5：其他参数
         * */
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 发消息
        String message = "hello world";
        /*
         * 发送一个消息
         * 参数1：发送到哪个交换机
         * 参数2：路由的key值是那个，本次是队列的名称
         * 参数3：其他参数信息
         * 参数4：发送消息的消息体
         * */
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
        System.out.println("消息发送完毕！");

    }
}


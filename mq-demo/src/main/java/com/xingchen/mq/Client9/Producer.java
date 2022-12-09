package com.xingchen.mq.Client9;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Client9
 * @date 2022/12/9 17:18
 */
public class Producer {
    // 队列名称
    public static  final String QUEUE_NAME="hello";

    // 发消息
    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();

        // 工厂IP连接RabbitMQ的队列
        factory.setHost("192.168.163.128");
        // 用户名
        factory.setUsername("admin");
        // 密码
        factory.setPassword("123");

        // 创建连接
        Connection connection = factory.newConnection();
        // 获取信道
        Channel channel = connection.createChannel();

        Map<String, Object> arguments = new HashMap<>();
        //官方允许是0-255之间，此处设置10，允许优化级范围为0-10，不要设置过大，浪费CPU与内存
        arguments.put("x-max-priority",10);
        channel.queueDeclare(QUEUE_NAME,true,false,false,arguments);
        // 发消息
        for (int i = 0; i < 10; i++) {
            String message = "info" + i;
            if(i == 5){
                AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().priority(5).build();
                channel.basicPublish("",QUEUE_NAME,properties,message.getBytes(StandardCharsets.UTF_8));
            }else {
                channel.basicPublish("",QUEUE_NAME,null,message.getBytes(StandardCharsets.UTF_8));

            }
        }
        System.out.println("消息发送完毕！");
    }
}


package com.xingchen.mq.utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.utils
 * @date 2022/12/4 16:07
 */
/*
 * 此类为连接工厂创建信道的工具类
 * */
public class RabbitMqUtils {
    // 得到一个连接的channel
    public static Channel getChannel() throws IOException, TimeoutException {
        // 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("ip");
        factory.setUsername("admin");
        factory.setPassword("123456");
        Connection connection = factory.newConnection();
        com.rabbitmq.client.Channel channel = connection.createChannel();
        return channel;
    }
}

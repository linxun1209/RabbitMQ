package com.xingchen.mq.Client5;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.xingchen.mq.utils.RabbitMqUtils;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Client5
 * @date 2022/12/6 21:10
 */
/*
 * 消息接收
 * */
public class ReceiveLogs02 {

    //交换机名称
    public static final String EXCHANGE_NAME = "logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMqUtils.getChannel();

        //声明一个队列,名称随机，当消费者断开与队列的连接时，队列自动删除
//        String queueName = channel.queueDeclare().getQueue();
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

        String queueName = channel.queueDeclare().getQueue();
        //绑定交换机与队列
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        System.out.println("等待接受消息，把接受到的消息打印在屏幕上...");


        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println("ReceiveLogs02控制台打印接受到的消息：" + new String(message.getBody(), "utf-8"));
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }
}


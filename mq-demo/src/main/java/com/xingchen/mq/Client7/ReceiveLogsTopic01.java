package com.xingchen.mq.Client7;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.xingchen.mq.utils.RabbitMqUtils;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Client7
 * @date 2022/12/6 22:33
 */
/*
 * 声明主题交换机及相关队列
 * 消费者C2
 * */
/*
 * 声明主题交换机及相关队列
 * 消费者C1
 * */
public class ReceiveLogsTopic01 {
    //交换机名称
    public static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMqUtils.getChannel();

        //声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        //声明队列
        String queueName = "Q1";
        channel.queueDeclare(queueName, false, false, false, null);

        //队列捆绑
        channel.queueBind(queueName, EXCHANGE_NAME, "*.orange.*");
        System.out.println("等待接收消息......");

        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println(new String(message.getBody()));
            System.out.println("接收队列：" + queueName + "绑定键：" + message.getEnvelope().getRoutingKey());
        };

        //接收消息
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }
}

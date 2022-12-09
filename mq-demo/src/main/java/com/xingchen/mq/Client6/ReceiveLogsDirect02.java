package com.xingchen.mq.Client6;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.xingchen.mq.utils.RabbitMqUtils;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Client6
 * @date 2022/12/6 22:18
 */
public class ReceiveLogsDirect02 {
    public static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMqUtils.getChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);


        //声明一个队列
        channel.queueDeclare("disk", false, false, false, null);

        //绑定交换机与队列
        channel.queueBind("disk", EXCHANGE_NAME, "disk");
//        channel.queueBind("console",EXCHANGE_NAME,"warning");

        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println("ReceiveLogsDirect02控制台打印接受到的消息：" + new String(message.getBody()));
        };

        channel.basicConsume("console", true, deliverCallback, consumerTag -> {
        });
    }
}


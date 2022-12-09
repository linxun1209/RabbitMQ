package com.xingchen.mq.Client6;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.xingchen.mq.utils.RabbitMqUtils;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.Client6
 * @date 2022/12/6 22:12
 */
public class DirectLogs {
    // 交换机的名称
    public static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMqUtils.getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String message = scanner.next();
            channel.basicPublish(EXCHANGE_NAME, "info", null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("生产者发出的消息：" + message);
        }
    }
}

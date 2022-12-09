package com.xingchen.mq.CLient3;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.xingchen.mq.utils.RabbitMqUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.CLient3
 * @date 2022/12/4 16:39
 */
/*
 * 消息在手动应答时是不丢失、放回队列中重新消费
 * */
public class Task2 {

    // 队列名称
    public static final String TASK_QUEUE_NAME = "ack_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        Channel channel = RabbitMqUtils.getChannel();

/**
 * 队列持久化
 */
//        // 声明队列
//        // 持久化 需要让Queue持久化
//        boolean durable = true;
//        channel.queueDeclare(TASK_QUEUE_NAME,durable,false,false,null);


        // 声明队列
        channel.queueDeclare(TASK_QUEUE_NAME, false, false, false, null);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {


            String message = scanner.next();
/**
 * 消息持久化
 */

//            //设置生产者发送消息为持久化消息（要求保存到磁盘上）
//            channel.basicPublish("",TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN
//                    ,message.getBytes(StandardCharsets.UTF_8));
//            System.out.println("生产者发出消息："+message);

            channel.basicPublish("", TASK_QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("生产者发出消息：" + message);
        }

    }
}


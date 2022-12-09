package com.xingchen.mq.CLient3;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.xingchen.mq.utils.RabbitMqUtils;
import com.xingchen.mq.utils.SleepUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.CLient3
 * @date 2022/12/4 16:40
 */
/*
 * 消费者
 * */
public class Worker03 {
    // 队列名称
    public static final String TASK_QUEUE_NAME = "ack_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        Channel channel = RabbitMqUtils.getChannel();
        System.out.println("C1等待接受消息处理时间较短");

        DeliverCallback deliverCallback = (consumerTag, message) -> {
            // 沉睡一秒
            SleepUtils.sleep(1);
            System.out.println("接受到的消息是:" + new String(message.getBody()));

            //进行手动应答
            /*
             * 参数1：消息的标记  tag
             * 参数2：是否批量应答，false：不批量应答 true：批量
             * */
            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
        };

///**
// * // 设置不公平分发
// */
//
//        int prefetchCount = 1;
//        channel.basicQos(prefetchCount);


        // 采用手动应答
        boolean autoAck = false;
        channel.basicConsume(TASK_QUEUE_NAME, autoAck, deliverCallback, (consumerTag) -> {
            System.out.println(consumerTag + "消费者取消消费接口回调逻辑");
        });
    }
}

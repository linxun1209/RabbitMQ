package com.xingchen.mq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Component
@Slf4j
public class WarningConsumer {
    @RabbitListener(queues = {"warning-queue"})
    public void receiveWarningMsg(Message message) {
        log.warn("出现不可路由消息:", message);
    }
}

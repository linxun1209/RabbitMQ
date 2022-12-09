package com.xingchen.mq.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Slf4j
@RestController
@RequestMapping("/confirm")
public class ConfirmController {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/sendConfirm/{msg}")
    public void sendConfirmMessage(@PathVariable("msg") String msg) {
        /**声明回调的形参*/
        CorrelationData correlationData = new CorrelationData("1");
        rabbitTemplate.convertAndSend("confirm-exchange", "confirm-key", msg, correlationData);
        log.info("发送信息为:" + msg);

        /**声明回调的形参*/
        CorrelationData correlationData2 = new CorrelationData("12");
        rabbitTemplate.convertAndSend("confirm-exchange", "confirm-key", msg, correlationData);
        log.info("发送信息为:" + msg);
    }
}

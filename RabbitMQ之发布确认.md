在生产环境中由于一些不明原因，导致 rabbitmq 重启，在 RabbitMQ 重启期间生产者消息投递失败， 
导致消息丢失，需要手动处理和恢复。于是，我们开始思考，如何才能进行 RabbitMQ 的消息可靠投递呢？
特别是在这样比较极端的情况，RabbitMQ 集群不可用的时候，无法投递的消息该如何处理呢
## **8.1. 发布确认 springboot 版本**
### **8.1.1. 确认机制方案**![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670571400518-f5ebfa1a-8b25-4fe3-8d4f-cbcbc69d787a.png#averageHue=%23f3e5d4&clientId=u60d01c86-5e18-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=258&id=u95c0671c&margin=%5Bobject%20Object%5D&name=image.png&originHeight=465&originWidth=1464&originalType=binary&ratio=1&rotation=0&showTitle=false&size=265498&status=done&style=none&taskId=u5b1c638e-9bca-4369-96c8-70124b45a22&title=&width=813.3333548793092)
### **8.1.2. 代码架构图**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670571420538-8fafb7a0-6604-4905-8e68-9af08c354057.png#averageHue=%23fdfbf9&clientId=u60d01c86-5e18-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=143&id=u044d90f2&margin=%5Bobject%20Object%5D&name=image.png&originHeight=257&originWidth=1424&originalType=binary&ratio=1&rotation=0&showTitle=false&size=52497&status=done&style=none&taskId=ufed93086-707b-4cc7-a820-27a5aa3aaf0&title=&width=791.1111320683991)
### **8.1.3. 配置文件**
在配置文件当中需要添加 
> **spring.rabbitmq.publisher-confirm-type=correlated **

⚫ NONE 
禁用发布确认模式，是默认值 
⚫ CORRELATED 
发布消息成功到交换器后会触发回调方法 
⚫ SIMPLE 
> 经测试有两种效果，其一效果和 CORRELATED 值一样会触发回调方法， 
> 其二在发布消息成功后使用 rabbitTemplate 调用 waitForConfirms 或 waitForConfirmsOrDie 方法 
> 等待 broker 节点返回发送结果，根据返回结果来判定下一步的逻辑，要注意的点是 
> waitForConfirmsOrDie 方法如果返回 false 则会关闭 channel，则接下来无法发送消息到 broker

### **8.1.4. 添加配置类**
```
package com.xingchen.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author xingchen
 * @version V1.0
 *  * 发布确认兜底方案  添加缓存测试
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Configuration
public class ConfirmConfig {

    //交换机
    private static final String CONFIRM_EXCHANGE_NAME = "confirm-exchange";
    //队列

    private static final String CONFIRM_QUEUE_NAME = "confirm-queue";
    //ROUTING_KEY

    private static final String CONFIRM_ROUTING_KEY = "confirm-key";

    /**
     * 备份交换机
     */
    private static final String BACKUP_EXCHANGE_NAME = "backup-exchange";
    /**
     * 备份队列
     */
    private static final String BACKUP_QUEUE_NAME = "backup-queue";
    /**
     * 报警队列
     */
    private static final String WARNING_QUEUE_NAME = "warning-queue";

    @Bean("confirmExchange")
    public DirectExchange directExchange() {
        /**确认交换机配置备份交换机 以确保宕机后将消息转发到备份交换机*/
        return ExchangeBuilder.directExchange(CONFIRM_EXCHANGE_NAME).durable(true)
                .withArgument("alternate-exchange", BACKUP_EXCHANGE_NAME).build();
    }

    @Bean("backupExchange")
    public FanoutExchange backupExchange() {
        return new FanoutExchange(BACKUP_EXCHANGE_NAME);
    }

    @Bean("confirmQueue")
    public Queue confirmQueue() {
        HashMap<String, Object> map = new HashMap<>(8);
        return new Queue(CONFIRM_QUEUE_NAME, false, false, false, map);
    }

    @Bean("backupQueue")
    public Queue backupQueue() {
        HashMap<String, Object> map = new HashMap<>(8);
        return new Queue(BACKUP_QUEUE_NAME, false, false, false, map);
    }

    @Bean("warningQueue")
    public Queue warningQueue() {
        HashMap<String, Object> map = new HashMap<>(8);
        return new Queue(WARNING_QUEUE_NAME, false, false, false, map);
    }

    @Bean
    public Binding queueConfirmBindingExchange(@Qualifier("confirmQueue") Queue queue,
                                               @Qualifier("confirmExchange") Exchange exchange) {

        return BindingBuilder.bind(queue).to(exchange).with(CONFIRM_ROUTING_KEY).noargs();
    }

    @Bean
    public Binding backupConfirmBindingExchange(@Qualifier("backupQueue") Queue queue,
                                                @Qualifier("backupExchange") FanoutExchange exchange) {

        return BindingBuilder.bind(queue).to(exchange);
    }

    @Bean
    public Binding warningConfirmBindingExchange(@Qualifier("warningQueue") Queue queue,
                                                 @Qualifier("backupExchange") FanoutExchange exchange) {

        return BindingBuilder.bind(queue).to(exchange);
    }
}

```
### **8.1.5. 消息生产者**
```
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
    }
}

```
### **8.1.6. 回调接口**
```
package com.xingchen.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Component
@Slf4j
public class MyCallBack implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 该注解会在其他注解执行完毕之后，进行一个属性的注入，必须将该类注入到rabbitTemplate的内部类中
     * 内部类就是这个ConfirmCallback
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        /**同时需要注入队列回退接口*/
        rabbitTemplate.setReturnCallback(this);
    }

    /**
     * @param correlationData 包含了消息的ID和其他数据信息 这个需要在发送方创建，否则没有
     * @param ack             返回的一个交换机确认状态 true 为确认 false 为未确认
     * @param cause           未确认的一个原因，如果ack为true的话，此值为null
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String id = correlationData != null ? correlationData.getId() : "";
        if (ack) {
            log.info("消息发送成功，id 是{} ", id);
        } else {
            log.info("消息发送失败，原因 是{} id 为{}", cause, id);
        }
    }
    /**
     * 可以在消息传递过程中，如果交换机遇到不可路由的情况，会将消息返回给生产者
     *
     * @param message    消息
     * @param replyCode  回复状态码
     * @param replyText  退回原因
     * @param exchange   交换机
     * @param routingKey 路由Key
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        log.error("消息{}，被交换机{}退回，路由Key是{}，退回原因是{}", new String(message.getBody()), exchange
                , routingKey, replyText);
    }
}

```
### **8.1.7. 消息消费者**
```
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
public class ConfirmConsumer {
    @RabbitListener(queues = {"confirm-queue"})
    public void receiveMsg(Message message) {
        log.info("接收到的消息为: " + new String(message.getBody()));
    }
}

```
### **8.1.8. 结果分析**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670572622613-40e071a9-50b4-4640-a59c-b4dee9448961.png#averageHue=%237a6f4e&clientId=u60d01c86-5e18-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=340&id=ucaf4e1df&margin=%5Bobject%20Object%5D&name=image.png&originHeight=612&originWidth=2625&originalType=binary&ratio=1&rotation=0&showTitle=false&size=1403975&status=done&style=none&taskId=u2f75ccbe-8ea7-4ffd-85c5-d21bba46373&title=&width=1458.3333719659745)
> 可以看到，发送了两条消息，第一条消息的 RoutingKey 为 "key1"，第二条消息的 RoutingKey 为 
> "key2"，两条消息都成功被交换机接收，也收到了交换机的确认回调，但消费者只收到了一条消息，因为 
> 第二条消息的 RoutingKey 与队列的 BindingKey 不一致，也没有其它队列能接收这个消息，所有第二条 
> 消息被直接丢弃了。

## **8.2. 回退消息**
### **8.2.1. Mandatory 参数 **
**在仅开启了生产者确认机制的情况下，交换机接收到消息后，会直接给消息生产者发送确认消息**，**如 **
**果发现该消息不可路由，那么消息会被直接丢弃，此时生产者是不知道消息被丢弃这个事件的**。那么如何 
让无法被路由的消息帮我想办法处理一下？最起码通知我一声，我好自己处理啊。通过设置 mandatory 参 
数可以在当消息传递过程中不可达目的地时将消息返回给生产者。 
### **8.2.2. 消息生产者代码**
```
package com.xingchen.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Component
@Slf4j
public class MyCallBack implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 该注解会在其他注解执行完毕之后，进行一个属性的注入，必须将该类注入到rabbitTemplate的内部类中
     * 内部类就是这个ConfirmCallback
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        /**同时需要注入队列回退接口*/
        rabbitTemplate.setReturnCallback(this);
    }

    /**
     * @param correlationData 包含了消息的ID和其他数据信息 这个需要在发送方创建，否则没有
     * @param ack             返回的一个交换机确认状态 true 为确认 false 为未确认
     * @param cause           未确认的一个原因，如果ack为true的话，此值为null
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String id = correlationData != null ? correlationData.getId() : "";
        if (ack) {
            log.info("消息发送成功，id 是{} ", id);
        } else {
            log.info("消息发送失败，原因 是{} id 为{}", cause, id);
        }
    }

    /**
     * 可以在消息传递过程中，如果交换机遇到不可路由的情况，会将消息返回给生产者
     *
     * @param message    消息
     * @param replyCode  回复状态码
     * @param replyText  退回原因
     * @param exchange   交换机
     * @param routingKey 路由Key
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        log.error("消息{}，被交换机{}退回，路由Key是{}，退回原因是{}", new String(message.getBody()), exchange
                , routingKey, replyText);
    }
}

```
### **8.2.3. 回调接口**
```
package com.xingchen.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Component
@Slf4j
public class MyCallBack implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 该注解会在其他注解执行完毕之后，进行一个属性的注入，必须将该类注入到rabbitTemplate的内部类中
     * 内部类就是这个ConfirmCallback
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        /**同时需要注入队列回退接口*/
        rabbitTemplate.setReturnCallback(this);
    }

    /**
     * @param correlationData 包含了消息的ID和其他数据信息 这个需要在发送方创建，否则没有
     * @param ack             返回的一个交换机确认状态 true 为确认 false 为未确认
     * @param cause           未确认的一个原因，如果ack为true的话，此值为null
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String id = correlationData != null ? correlationData.getId() : "";
        if (ack) {
            log.info("消息发送成功，id 是{} ", id);
        } else {
            log.info("消息发送失败，原因 是{} id 为{}", cause, id);
        }
    }

    /**
     * 可以在消息传递过程中，如果交换机遇到不可路由的情况，会将消息返回给生产者
     *
     * @param message    消息
     * @param replyCode  回复状态码
     * @param replyText  退回原因
     * @param exchange   交换机
     * @param routingKey 路由Key
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        log.error("消息{}，被交换机{}退回，路由Key是{}，退回原因是{}", new String(message.getBody()), exchange
                , routingKey, replyText);
    }
}

```
### **8.2.4. 结果分析**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670573046826-627a5289-5795-4cb3-a23e-b92a2055b834.png#averageHue=%23dfdfdb&clientId=u60d01c86-5e18-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=146&id=ue4d75f24&margin=%5Bobject%20Object%5D&name=image.png&originHeight=263&originWidth=1587&originalType=binary&ratio=1&rotation=0&showTitle=false&size=517379&status=done&style=none&taskId=uc5113b53-739a-4ef0-8e87-fe852c4c6a6&title=&width=881.6666900228577)
## **8.3. 备份交换机**
> 有了 mandatory 参数和回退消息，我们获得了对无法投递消息的感知能力，有机会在生产者的消息 
> 无法被投递时发现并处理。但有时候，我们并不知道该如何处理这些无法路由的消息，最多打个日志，然 
> 后触发报警，再来手动处理。而通过日志来处理这些无法路由的消息是很不优雅的做法，特别是当生产者 
> 所在的服务有多台机器的时候，手动复制日志会更加麻烦而且容易出错。而且设置 mandatory 参数会增 
> 加生产者的复杂性，需要添加处理这些被退回的消息的逻辑。如果既不想丢失消息，又不想增加生产者的 
> 复杂性，该怎么做呢？前面在设置死信队列的文章中，我们提到，可以为队列设置死信交换机来存储那些 
> 处理失败的消息，可是这些不可路由消息根本没有机会进入到队列，因此无法使用死信队列来保存消息。 
> 在 RabbitMQ 中，有一种备份交换机的机制存在，可以很好的应对这个问题。什么是备份交换机呢？备份 
> 交换机可以理解为 RabbitMQ 中交换机的“备胎”，当我们为某一个交换机声明一个对应的备份交换机时，就 
> 是为它创建一个备胎，当交换机接收到一条不可路由消息时，将会把这条消息转发到备份交换机中，由备 
> 份交换机来进行转发和处理，通常备份交换机的类型为 Fanout ，这样就能把所有消息都投递到与其绑定 
> 的队列中，然后我们在备份交换机下绑定一个队列，这样所有那些原交换机无法被路由的消息，就会都进 
> 入这个队列了。当然，我们还可以建立一个报警队列，用独立的消费者来进行监测和报警。 

### **8.3.1. 代码架构图**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670573211962-faccdb23-4401-4401-a936-ecb288d4d3b3.png#averageHue=%23fdfbf9&clientId=u60d01c86-5e18-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=345&id=ufd0f1a72&margin=%5Bobject%20Object%5D&name=image.png&originHeight=621&originWidth=1651&originalType=binary&ratio=1&rotation=0&showTitle=false&size=157964&status=done&style=none&taskId=u3b7d1400-f3b9-43df-852e-773f318ff59&title=&width=917.2222465203139)
### **8.3.2. 修改配置类**
```
package com.xingchen.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author xingchen
 * @version V1.0
 *  * 发布确认兜底方案  添加缓存测试
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Configuration
public class ConfirmConfig {

    //交换机
    private static final String CONFIRM_EXCHANGE_NAME = "confirm-exchange";
    //队列

    private static final String CONFIRM_QUEUE_NAME = "confirm-queue";
    //ROUTING_KEY

    private static final String CONFIRM_ROUTING_KEY = "confirm-key";

    /**
     * 备份交换机
     */
    private static final String BACKUP_EXCHANGE_NAME = "backup-exchange";
    /**
     * 备份队列
     */
    private static final String BACKUP_QUEUE_NAME = "backup-queue";
    /**
     * 报警队列
     */
    private static final String WARNING_QUEUE_NAME = "warning-queue";

    @Bean("confirmExchange")
    public DirectExchange directExchange() {
        /**确认交换机配置备份交换机 以确保宕机后将消息转发到备份交换机*/
        return ExchangeBuilder.directExchange(CONFIRM_EXCHANGE_NAME).durable(true)
                .withArgument("alternate-exchange", BACKUP_EXCHANGE_NAME).build();
    }

    @Bean("backupExchange")
    public FanoutExchange backupExchange() {
        return new FanoutExchange(BACKUP_EXCHANGE_NAME);
    }

    @Bean("confirmQueue")
    public Queue confirmQueue() {
        HashMap<String, Object> map = new HashMap<>(8);
        return new Queue(CONFIRM_QUEUE_NAME, false, false, false, map);
    }

    @Bean("backupQueue")
    public Queue backupQueue() {
        HashMap<String, Object> map = new HashMap<>(8);
        return new Queue(BACKUP_QUEUE_NAME, false, false, false, map);
    }

    @Bean("warningQueue")
    public Queue warningQueue() {
        HashMap<String, Object> map = new HashMap<>(8);
        return new Queue(WARNING_QUEUE_NAME, false, false, false, map);
    }

    @Bean
    public Binding queueConfirmBindingExchange(@Qualifier("confirmQueue") Queue queue,
                                               @Qualifier("confirmExchange") Exchange exchange) {

        return BindingBuilder.bind(queue).to(exchange).with(CONFIRM_ROUTING_KEY).noargs();
    }

    @Bean
    public Binding backupConfirmBindingExchange(@Qualifier("backupQueue") Queue queue,
                                                @Qualifier("backupExchange") FanoutExchange exchange) {

        return BindingBuilder.bind(queue).to(exchange);
    }

    @Bean
    public Binding warningConfirmBindingExchange(@Qualifier("warningQueue") Queue queue,
                                                 @Qualifier("backupExchange") FanoutExchange exchange) {

        return BindingBuilder.bind(queue).to(exchange);
    }
}

```
### **8.3.3. 报警消费者**
```
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

```
### **8.3.4. 测试注意事项**
> 重新启动项目的时候需要把原来的confirm.exchange 删除因为我们修改了其绑定属性，不然报以下错:

![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670575413647-b14b57b0-c8f5-4eca-ad9a-d1455228db66.png#averageHue=%23f7f5f2&clientId=u60d01c86-5e18-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=62&id=u460db662&margin=%5Bobject%20Object%5D&name=image.png&originHeight=112&originWidth=1664&originalType=binary&ratio=1&rotation=0&showTitle=false&size=88227&status=done&style=none&taskId=u4e37f005-0779-42b0-938c-a49d609a429&title=&width=924.4444689338596)
### **8.3.5. 结果分析**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670575441420-5093159f-fbc5-4a1c-a83c-8d3a7899e89c.png#averageHue=%23dededb&clientId=u60d01c86-5e18-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=197&id=u6b89c003&margin=%5Bobject%20Object%5D&name=image.png&originHeight=355&originWidth=1669&originalType=binary&ratio=1&rotation=0&showTitle=false&size=621720&status=done&style=none&taskId=u0643139b-aab7-4c71-8ba8-b6a1a2601c8&title=&width=927.2222467852234)
> mandatory 参数与备份交换机可以一起使用的时候，如果两者同时开启，消息究竟何去何从？谁优先 
> 级高，经过上面结果显示答案是**备份交换机优先级高**。


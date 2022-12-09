# **7. 延迟队列**
## **7.1. 延迟队列概念**
延时队列,队列内部是有序的，最重要的特性就体现在它的延时属性上，延时队列中的元素是希望 
在指定时间到了以后或之前取出和处理，简单来说，延时队列就是用来存放需要在指定时间被处理的 
元素的队列。 
## **7.2. 延迟队列使用场景 **
> - 1.订单在十分钟之内未支付则自动取消 
> - 2.新创建的店铺，如果在十天内都没有上传过商品，则自动发送消息提醒。 
> - 3.用户注册成功后，如果三天内没有登陆则进行短信提醒。 
> - 4.用户发起退款，如果三天内没有得到处理则通知相关运营人员。 
> - 5.预定会议后，需要在预定的时间点前十分钟通知各个与会人员参加会议

这些场景都有一个特点，需要在某个事件发生之后或者之前的指定时间点完成某一项任务，如： 
发生订单生成事件，在十分钟之后检查该订单支付状态，然后将未支付的订单进行关闭；看起来似乎 
使用定时任务，一直轮询数据，每秒查一次，取出需要被处理的数据，然后处理不就完事了吗？如果 
数据量比较少，确实可以这样做，比如：对于“如果账单一周内未支付则进行自动结算”这样的需求， 
如果对于时间不是严格限制，而是宽松意义上的一周，那么每天晚上跑个定时任务检查一下所有未支 
付的账单，确实也是一个可行的方案。但对于数据量比较大，并且时效性较强的场景，如：“订单十 
分钟内未支付则关闭“，短期内未支付的订单数据可能会有很多，活动期间甚至会达到百万甚至千万 
级别，对这么庞大的数据量仍旧使用轮询的方式显然是不可取的，很可能在一秒内无法完成所有订单 
的检查，同时会给数据库带来很大压力，无法满足业务要求而且性能低下。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670386308311-5d218224-cc0b-466e-9783-68b8004c960c.png#averageHue=%23f4f4f4&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=586&id=u5b5cfef7&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1172&originWidth=1322&originalType=binary&ratio=1&rotation=0&showTitle=false&size=416104&status=done&style=none&taskId=u63eaa299-a1ff-46e3-8617-ae3aa128053&title=&width=661)
## **7.3. RabbitMQ 中的 TTL **
TTL 是什么呢？TTL 是 RabbitMQ 中一个消息或者队列的属性，表明一条消息或者该队列中的所有 
消息的最大存活时间，单位是毫秒。换句话说，如果一条消息设置了 TTL 属性或者进入了设置TTL 属性的队列，那么这 条消息如果在TTL 设置的时间内没有被消费，则会成为"死信"。如果同时配置了队列的TTL 和消息的 TTL，那么较小的那个值将会被使用，有两种方式设置 TTL。 
### **7.3.1. 消息设置TTL**
另一种方式便是针对每条消息设置TTL
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670386356823-a985446c-c7b8-45e8-828b-85879d305e9e.png#averageHue=%23c4e9c9&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=75&id=u3f3c0bce&margin=%5Bobject%20Object%5D&name=image.png&originHeight=150&originWidth=1685&originalType=binary&ratio=1&rotation=0&showTitle=false&size=94738&status=done&style=none&taskId=ud6a4dce6-3172-4f24-a8f3-1984abbf48d&title=&width=842.5)
### **7.3.2. 队列设置TTL**
第一种是在创建队列的时候设置队列的“ x-message-ttl”属性
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670386395898-edc2d7f4-b2ad-4833-bc72-70630bcc33a1.png#averageHue=%23bbdfbf&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=71&id=u2d811b73&margin=%5Bobject%20Object%5D&name=image.png&originHeight=142&originWidth=1602&originalType=binary&ratio=1&rotation=0&showTitle=false&size=237263&status=done&style=none&taskId=u7a7315fb-4497-4824-9742-b12ef56c584&title=&width=801)
### **7.3.3. 两者的区别**
如果设置了队列的 TTL 属性，那么一旦消息过期，就会被队列丢弃(如果配置了死信队列被丢到死信队 
列中)，而第二种方式，消息即使过期，也不一定会被马上丢弃，因为**消息是否过期是在即将投递到消费者 **
**之前判定的**，如果当前队列有严重的消息积压情况，则已过期的消息也许还能存活较长时间；另外，还需 
要注意的一点是，如果不设置 TTL，表示消息永远不会过期，如果将 TTL 设置为 0，则表示除非此时可以 
直接投递该消息到消费者，否则该消息将会被丢弃。 
前一小节我们介绍了死信队列，刚刚又介绍了 TTL，至此利用 RabbitMQ 实现延时队列的两大要素已 
经集齐，接下来只需要将它们进行融合，再加入一点点调味料，延时队列就可以新鲜出炉了。想想看，延 
时队列，不就是想要消息延迟多久被处理吗，TTL 则刚好能让消息在延迟多久之后成为死信，另一方面， 
成为死信的消息都会被投递到死信队列里，这样只需要消费者一直消费死信队列里的消息就完事了，因为 
里面的消息都是希望被立即处理的消息。
### **7.4. 整合 springboot**
#### pom.xml
```
<dependencies>
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter</artifactId>
     </dependency>

     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-test</artifactId>
         <scope>test</scope>
     </dependency>

     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-amqp</artifactId>
     </dependency>

     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-web</artifactId>
     </dependency>

     <dependency>
         <groupId>com.alibaba</groupId>
         <artifactId>fastjson</artifactId>
         <version>1.2.73</version>
     </dependency>

     <dependency>
         <groupId>org.projectlombok</groupId>
         <artifactId>lombok</artifactId>
     </dependency>

     <dependency>
         <groupId>io.springfox</groupId>
         <artifactId>springfox-swagger2</artifactId>
         <version>2.9.2</version>
     </dependency>

     <dependency>
         <groupId>io.springfox</groupId>
         <artifactId>springfox-swagger-ui</artifactId>
         <version>2.9.2</version>
     </dependency>

     <dependency>
         <groupId>org.springframework.amqp</groupId>
         <artifactId>spring-rabbit-test</artifactId>
         <scope>test</scope>
     </dependency>
 </dependencies>

```
#### 配置文件
```
 spring.rabbitmq.host=192.168.163,128
 spring.rabbitmq.port=5672
 spring.rabbitmq.username=admin
 spring.rabbitmq.password=123
```
#### 添加Swagger配置类
```
 @Configuration
 @EnableSwagger2
 public class SwaggerConfig {
     @Bean
     public Docket webApiConfig(){
         return new Docket(DocumentationType.SWAGGER_2)
                 .groupName("webApi")
                 .apiInfo(webApiInfo())
                 .select()
                 .build();
     }

 private ApiInfo webApiInfo(){
     return new ApiInfoBuilder()
             .title("rabbitmq接口文档")
             .description("本文档描述了rabbitmq微服务接口定义")
             .version("1.0")
             .contact(new Contact("enjoy6288","http://atguigu.com","123456@qq.com"))
             .build();
 }
}

```
### **7.5. 队列 TTL**
#### **7.5.1. 代码架构图**
创建两个队列 QA 和 QB，两者队列 TTL 分别设置为 10S 和 40S，然后在创建一个交换机 X 和死信交 
换机 Y，它们的类型都是direct，创建一个死信队列 QD，它们的绑定关系如下
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670386606013-a3b61187-2c82-4c1d-8167-82649ab8b123.png#averageHue=%23f9f8f7&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=162&id=u3aa16717&margin=%5Bobject%20Object%5D&name=image.png&originHeight=324&originWidth=1689&originalType=binary&ratio=1&rotation=0&showTitle=false&size=73331&status=done&style=none&taskId=ud4d141fb-ca44-4df0-a2b4-b51191f63c5&title=&width=844.5)
#### **7.5.2. 配置文件类代码**
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
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Configuration
public class TtlQueueConfig {

    /**
     * 普通交换机
     */
    private static final String X_EXCHANGE = "X";
    /**
     * 死信交换机
     */
    private static final String Y_DEAD_LETTER_EXCHANGE = "Y";
    /**
     * 普通队列
     */
    private static final String QUEUE_A = "QA";
    private static final String QUEUE_B = "QB";
    /**
     * 死信队列
     */
    private static final String DEAD_LETTER_QUEUE = "QD";

    /**
     * 新的普通队列
     */
    private static final String QUEUE_C = "QC";


    /**
     * 声明XExchange  别名
     */
    @Bean("xExchange")
    public DirectExchange xDirectExchange() {
        return new DirectExchange(X_EXCHANGE);
    }

    /**
     * 声明XExchange  别名
     */
    @Bean("yExchange")
    public DirectExchange yDirectExchange() {
        return new DirectExchange(Y_DEAD_LETTER_EXCHANGE);
    }

    /**
     * 声明queueA
     */
    @Bean("queueA")
    public Queue queueA() {
        HashMap<String, Object> map = new HashMap<>(4);
        /**设置死信队列的交换机*/
        map.put("x-dead-letter-exchange", Y_DEAD_LETTER_EXCHANGE);
        /**设置死信队列的routingKey*/
        map.put("x-dead-letter-routing-key", "YD");
        /**设置过期时间*/
        map.put("x-message-ttl", 10000);

        return QueueBuilder.durable(QUEUE_A).withArguments(map).build();
    }

    /**
     * 声明queueB
     */
    @Bean("queueB")
    public Queue queueB() {
        HashMap<String, Object> map = new HashMap<>(4);
        /**设置死信队列的交换机*/
        map.put("x-dead-letter-exchange", Y_DEAD_LETTER_EXCHANGE);
        /**设置死信队列的routingKey*/
        map.put("x-dead-letter-routing-key", "YD");
        /**设置过期时间*/
        map.put("x-message-ttl", 40000);

        return QueueBuilder.durable(QUEUE_B).withArguments(map).build();
    }

    /**
     * 声明queueC
     * 用来存放发送者自定义的延迟队列 因此取消定义队列过期时间
     */
    @Bean("queueC")
    public Queue queueC() {
        HashMap<String, Object> map = new HashMap<>(4);
        /**设置死信队列的交换机*/
        map.put("x-dead-letter-exchange", Y_DEAD_LETTER_EXCHANGE);
        /**设置死信队列的routingKey*/
        map.put("x-dead-letter-routing-key", "YD");

        return QueueBuilder.durable(QUEUE_C).withArguments(map).build();
    }

    /**
     * 声明死信队列
     */
    @Bean("queueD")
    public Queue queueD() {

        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    /**
     * 队列和交换机绑定
     */
    @Bean
    public Binding queueABindingX(@Qualifier("queueA") Queue queueA,
                                  @Qualifier("xExchange") Exchange xExchange) {
        return BindingBuilder.bind(queueA).to(xExchange).with("XA").noargs();
    }

    @Bean
    public Binding queueBBindingX(@Qualifier("queueB") Queue queueB,
                                  @Qualifier("xExchange") Exchange xExchange) {
        return BindingBuilder.bind(queueB).to(xExchange).with("XB").noargs();
    }

    @Bean
    public Binding queueCBindingY(@Qualifier("queueC") Queue queueC,
                                  @Qualifier("xExchange") Exchange xExchange) {
        return BindingBuilder.bind(queueC).to(xExchange).with("XC").noargs();
    }

    @Bean
    public Binding queueDBindingY(@Qualifier("queueD") Queue queueD,
                                  @Qualifier("yExchange") Exchange yExchange) {
        return BindingBuilder.bind(queueD).to(yExchange).with("YD").noargs();
    }
}


```
### **7.5.3. 消息生产者代码**
```
package com.xingchen.mq.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.controller
 * @date 2022/12/7 11:53
 */
@RestController
@RequestMapping("/ttl")
@Slf4j
public class MsgController {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 开始发消息
     */
    @GetMapping("/sendMsg/{msg}")
    public void sendMsg(@PathVariable("msg") String msg) {
        /**后者会给占位符赋值，实现动态传递*/
        log.info("当前时间:{},发送一条消息给两个TTL队列", new Date().toString(), msg);
        rabbitTemplate.convertAndSend("X", "XA", "消息来自ttl为10s的队列:" + msg);
        rabbitTemplate.convertAndSend("X", "XB", "消息来自ttl为40s的队列:" + msg);
    }
}
```
**7.5.4. 消息消费者代码**
```
package com.xingchen.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.consumer
 * @date 2022/12/7 12:08
 */
@Component
@Slf4j
public class DeadLetterConsumer {

    /**
     * 接收消息
     */
    @RabbitListener(queues = {"QD"})
    public void receiveD(Message msg, Channel channel) throws Exception {
        String result = new String(msg.getBody());
        log.info("当前时间:{},收到死信队列的消息: {}", new Date(), result);
    }
}

```
#### 测试
> 发起一个请求 http://localhost:8080/ttl/sendMsg/嘻嘻嘻

![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670386763373-7d7ed2c1-d300-464a-b1c1-58e3f0d1663b.png#averageHue=%2384956a&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=184&id=uaf7f6d1c&margin=%5Bobject%20Object%5D&name=image.png&originHeight=367&originWidth=2631&originalType=binary&ratio=1&rotation=0&showTitle=false&size=923640&status=done&style=none&taskId=u34cde5bb-6a0b-43dc-81fe-e467eef6f1c&title=&width=1315.5)
第一条消息在 10S 后变成了死信消息，然后被消费者消费掉，第二条消息在 40S 之后变成了死信消息， 
然后被消费掉，这样一个延时队列就打造完成了。 
不过，如果这样使用的话，岂不是**每增加一个新的时间需求，就要新增一个队列**，这里只有 10S 和 40S 
两个时间选项，如果需要一个小时后处理，那么就需要增加TTL 为一个小时的队列，如果是预定会议室然 
后提前通知这样的场景，岂不是要增加无数个队列才能满足需求？ 
## **7.6. 延时队列优化**
### **7.6.1. 代码架构图**
在这里新增了一个队列 QC,绑定关系如下,该队列不设置TTL 时间
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670386853882-427eb554-2ef4-4f7e-8da5-55e2792e9c1d.png#averageHue=%23faf9f8&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=241&id=u4ec98ebc&margin=%5Bobject%20Object%5D&name=image.png&originHeight=482&originWidth=1647&originalType=binary&ratio=1&rotation=0&showTitle=false&size=89883&status=done&style=none&taskId=uf5a5b711-31d5-4c17-b013-4db97f13316&title=&width=823.5)
### **7.6.2. 配置文件类代码**
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
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 11:52
 */
@Configuration
public class TtlQueueConfig {

    /**
     * 普通交换机
     */
    private static final String X_EXCHANGE = "X";
    /**
     * 死信交换机
     */
    private static final String Y_DEAD_LETTER_EXCHANGE = "Y";
    /**
     * 普通队列
     */
    private static final String QUEUE_A = "QA";
    private static final String QUEUE_B = "QB";
    /**
     * 死信队列
     */
    private static final String DEAD_LETTER_QUEUE = "QD";

    /**
     * 新的普通队列
     */
    private static final String QUEUE_C = "QC";


    /**
     * 声明XExchange  别名
     */
    @Bean("xExchange")
    public DirectExchange xDirectExchange() {
        return new DirectExchange(X_EXCHANGE);
    }

    /**
     * 声明XExchange  别名
     */
    @Bean("yExchange")
    public DirectExchange yDirectExchange() {
        return new DirectExchange(Y_DEAD_LETTER_EXCHANGE);
    }

    /**
     * 声明queueA
     */
    @Bean("queueA")
    public Queue queueA() {
        HashMap<String, Object> map = new HashMap<>(4);
        /**设置死信队列的交换机*/
        map.put("x-dead-letter-exchange", Y_DEAD_LETTER_EXCHANGE);
        /**设置死信队列的routingKey*/
        map.put("x-dead-letter-routing-key", "YD");
        /**设置过期时间*/
        map.put("x-message-ttl", 10000);

        return QueueBuilder.durable(QUEUE_A).withArguments(map).build();
    }

    /**
     * 声明queueB
     */
    @Bean("queueB")
    public Queue queueB() {
        HashMap<String, Object> map = new HashMap<>(4);
        /**设置死信队列的交换机*/
        map.put("x-dead-letter-exchange", Y_DEAD_LETTER_EXCHANGE);
        /**设置死信队列的routingKey*/
        map.put("x-dead-letter-routing-key", "YD");
        /**设置过期时间*/
        map.put("x-message-ttl", 40000);

        return QueueBuilder.durable(QUEUE_B).withArguments(map).build();
    }

    /**
     * 声明queueC
     * 用来存放发送者自定义的延迟队列 因此取消定义队列过期时间
     */
    @Bean("queueC")
    public Queue queueC() {
        HashMap<String, Object> map = new HashMap<>(4);
        /**设置死信队列的交换机*/
        map.put("x-dead-letter-exchange", Y_DEAD_LETTER_EXCHANGE);
        /**设置死信队列的routingKey*/
        map.put("x-dead-letter-routing-key", "YD");

        return QueueBuilder.durable(QUEUE_C).withArguments(map).build();
    }

    /**
     * 声明死信队列
     */
    @Bean("queueD")
    public Queue queueD() {

        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    /**
     * 队列和交换机绑定
     */
    @Bean
    public Binding queueABindingX(@Qualifier("queueA") Queue queueA,
                                  @Qualifier("xExchange") Exchange xExchange) {
        return BindingBuilder.bind(queueA).to(xExchange).with("XA").noargs();
    }

    @Bean
    public Binding queueBBindingX(@Qualifier("queueB") Queue queueB,
                                  @Qualifier("xExchange") Exchange xExchange) {
        return BindingBuilder.bind(queueB).to(xExchange).with("XB").noargs();
    }

    @Bean
    public Binding queueCBindingY(@Qualifier("queueC") Queue queueC,
                                  @Qualifier("xExchange") Exchange xExchange) {
        return BindingBuilder.bind(queueC).to(xExchange).with("XC").noargs();
    }

    @Bean
    public Binding queueDBindingY(@Qualifier("queueD") Queue queueD,
                                  @Qualifier("yExchange") Exchange yExchange) {
        return BindingBuilder.bind(queueD).to(yExchange).with("YD").noargs();
    }
}


```
### 生产者
```
  /**
     * 开始发定义有过期时间的消息
     */
    @GetMapping("/sendMsg/{msg}/{ttlTime}")
    public void sendExpireMsg(@PathVariable("msg") String msg, @PathVariable("ttlTime") String ttlTime) {
        /**后者会给占位符赋值，实现动态传递*/
        log.info("当前时间:{},发送一条时长是{}ms的TTL消息给QC队列,内容是{}", new Date(), ttlTime, msg);
        rabbitTemplate.convertAndSend("X", "XC", "ttl消息为" + ttlTime + "的时间，内容为:" + msg, message -> {
            /**生产者设置过期时间*/
            message.getMessageProperties().setExpiration(ttlTime);
            return message;
        });
    }
```
#### 测试
http://localhost:8080/ttl/sendExpirationMsg/你好 1/20000 
http://localhost:8080/ttl/sendExpirationMsg/你好 2/2000
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670388130635-4d2c8e5f-5f5b-455f-992c-0a191b5dd9c6.png#averageHue=%23787854&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=278&id=ue45e57f6&margin=%5Bobject%20Object%5D&name=image.png&originHeight=555&originWidth=2724&originalType=binary&ratio=1&rotation=0&showTitle=false&size=1360543&status=done&style=none&taskId=ud036ef59-22e8-4241-ba5e-6cf8c98c7d3&title=&width=1362)
看起来似乎没什么问题，但是在最开始的时候，就介绍过如果使用在消息属性上设置 TTL 的方式，消 
息可能并不会按时“死亡“，因为 **RabbitMQ 只会检查第一个消息是否过期**，如果过期则丢到死信队列， 
**如果第一个消息的延时时长很长，而第二个消息的延时时长很短，第二个消息并不会优先得到执行**。 
## **7.7. Rabbitmq 插件实现延迟队列 **
上文中提到的问题，确实是一个问题，如果不能实现在消息粒度上的 TTL，并使其在设置的TTL 时间 
及时死亡，就无法设计成一个通用的延时队列。那如何解决呢，接下来我们就去解决该问题。 
### **7.7.1. 安装延时队列插件**
在官网上下载 https://www.rabbitmq.com/community-plugins.html，下载 
**rabbitmq_delayed_message_exchange **插件，然后解压放置到 RabbitMQ 的插件目录。 
进入 RabbitMQ 的安装目录下的 plgins 目录，执行下面命令让该插件生效，然后重启 RabbitMQ 
/usr/lib/rabbitmq/lib/rabbitmq_server-3.8.8/plugins 
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670388228014-d0d520e8-39d2-4b73-bd95-b420873d744a.png#averageHue=%23888787&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=456&id=ud978e02e&margin=%5Bobject%20Object%5D&name=image.png&originHeight=912&originWidth=1676&originalType=binary&ratio=1&rotation=0&showTitle=false&size=579149&status=done&style=none&taskId=udcecfe4e-9c2f-44a2-81c0-570ee0ff477&title=&width=838)
### **7.7.2. 代码架构图**
在这里新增了一个队列delayed.queue,一个自定义交换机 delayed.exchange，绑定关系如下:
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670390689104-1d65365f-f1ab-427c-9398-3cb7251e1d9b.png#averageHue=%23fefefe&clientId=u15a7518f-c24f-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=189&id=u89187073&margin=%5Bobject%20Object%5D&name=image.png&originHeight=378&originWidth=1646&originalType=binary&ratio=1&rotation=0&showTitle=false&size=60738&status=done&style=none&taskId=u4b789bb0-9821-4db3-b617-a4ddd3d44b9&title=&width=823)
### **7.7.3. 配置文件类代码**
在我们自定义的交换机中，这是一种新的交换类型，该类型消息支持延迟投递机制 消息传递后并 
不会立即投递到目标队列中，而是存储在 mnesia(一个分布式数据系统)表中，当达到投递时间时，才 
投递到目标队列中
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
 * @Package com.xingchen.mq.config
 * @date 2022/12/7 13:27
 */
@Configuration
public class DelayedQueueConfig {
    /**
     * 交换机
     * 队列
     * routingKey
     */
    private static final String DELAYED_EXCHANGE_NAME = "delayed.exchange";
    private static final String DELAYED_QUEUE_NAME = "delayed.queue";
    private static final String DELAYED_ROUTING_KEY = "delayed.routingkey";

    /**
     * 声明队列
     */
    @Bean
    public Queue delayedQueue() {
        return new Queue(DELAYED_QUEUE_NAME);
    }

    /**
     * 声明交换机 基于插件的
     */
    @Bean
    public CustomExchange delayedExchange() {
        /**
         * 1、交换机名称
         * 2、交换机类型
         * 3、是否持久化
         * 4、是否自动删除
         * 5、参数
         * */
        HashMap<String, Object> map = new HashMap<>(2);
        /**固定参数*/
        map.put("x-delayed-type", "direct");
        return new CustomExchange(DELAYED_EXCHANGE_NAME, "x-delayed-message", true, false, map);
    }

    /**
     * 绑定交换机和队列
     */
    @Bean
    public Binding queueBindingExchange(
            @Qualifier("delayedQueue") Queue queue,
            @Qualifier("delayedExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(DELAYED_ROUTING_KEY).noargs();
    }
}


```
### **7.7.4. 消息生产者代码**
```
    /**
     * 基于插件的延时队列
     */
    @GetMapping("/sendDelayedMsg/{msg}/{delayedTime}")
    public void sendDelayedMsg(@PathVariable("msg") String msg, @PathVariable("delayedTime") Integer delayedTime) {
        /**后者会给占位符赋值，实现动态传递*/
        log.info("当前时间:{},发送一条时长是{}ms的延时消息给QC队列,内容是{}", new Date(), delayedTime, msg);
        rabbitTemplate.convertAndSend("delayed.exchange", "delayed.routingkey",
                "延时消息的时间是" + delayedTime + "，内容为:" + msg, message -> {
                    /**生产者设置延时时间 */
                    /**和上面的有区别*/
                    message.getMessageProperties().setDelay(delayedTime);
                    return message;
                });
    }
```
### **7.7.5. 消息消费者代码**
```
package com.xingchen.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq.consumer
 * @date 2022/12/7 13:40
 */
@Component
@Slf4j
public class DelayedConsumer {
    @RabbitListener(queues = {"delayed.queue"})
    public void receiveMsg(Message message, Channel channel) {
        String msg = new String(message.getBody());
        log.info("当前时间为{}，收到延迟消息为{}", new Date(), msg);
    }
}
```
发起请求： 
http://localhost:8080/ttl/sendDelayMsg/come on baby1/20000 
http://localhost:8080/ttl/sendDelayMsg/come on baby2/2000 
第二个消息被先消费掉了，符合预期
## **7.8. 总结**
延时队列在需要延时处理的场景下非常有用，使用 RabbitMQ 来实现延时队列可以很好的利用 
RabbitMQ 的特性，如：消息可靠发送、消息可靠投递、死信队列来保障消息至少被消费一次以及未被正 
确处理的消息不会被丢弃。另外，通过 RabbitMQ 集群的特性，可以很好的解决单点故障问题，不会因为 
单个节点挂掉导致延时队列不可用或者消息丢失。 
当然，延时队列还有很多其它选择，比如利用 Java 的 DelayQueue，利用 Redis 的 zset，利用 Quartz 
或者利用 kafka 的时间轮，这些方式各有特点,看需要适用的场景

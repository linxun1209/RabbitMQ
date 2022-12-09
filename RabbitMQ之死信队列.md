# **6. 死信队列**
## **6.1. 死信的概念**
先从概念解释上搞清楚这个定义，死信，顾名思义就是无法被消费的消息，字面意思可以这样理 
解，一般来说，producer 将消息投递到 broker 或者直接到queue 里了，consumer 从 queue 取出消息 
进行消费，但某些时候由于特定的**原因导致 queue 中的某些消息无法被消费**，这样的消息如果没有 
后续的处理，就变成了死信，有死信自然就有了死信队列。 
**应用场景:**
为了保证订单业务的消息数据不丢失，需要使用到 RabbitMQ 的死信队列机制，当消息 
消费发生异常时，将消息投入死信队列中.还有比如说: 用户在商城下单成功并点击去支付后在指定时 
间未支付时自动失效 
## **6.2. 死信的来源**
消息 TTL 过期 
队列达到最大长度(队列满了，无法再添加数据到 mq 中) 
消息被拒绝(basic.reject 或 basic.nack)并且 requeue=false.
## **6.3. 死信实战**
### **6.3.1. 代码架构图**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670374364557-4b27d555-dba7-4a06-bd1f-e2c98e1555b7.png#averageHue=%23fbfafa&clientId=ub616990b-7e68-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=368&id=u3de77e44&margin=%5Bobject%20Object%5D&name=image.png&originHeight=735&originWidth=1636&originalType=binary&ratio=1&rotation=0&showTitle=false&size=134754&status=done&style=none&taskId=u1d320160-1ecd-4529-b467-316795f7cdc&title=&width=818)
### **6.3.2. 消息TTL 过期**
生产者代码
```
    /*
 * 死信队列之生产者代码
 *
 * */
 public class Producer {
 
     //普通交换机的名称
     public static final String NORMAL_EXCHANGE = "normal_exchange";
     public static void main(String[] args) throws Exception{
         Channel channel = RabbitMqUtils.getChannel();
 
         //死信消息，设置TTL时间  单位是ms  10000ms是10s
         AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().expiration("10000").build();
 
         for (int i = 0; i < 10; i++) {
             String message = "info" + i;
             channel.basicPublish(NORMAL_EXCHANGE,"zhangsan",properties,message.getBytes(StandardCharsets.UTF_8));
         }
     }
 }

```
消费者
```
    /*
 * 死信队列实战
 * 消费者01
 * */
 public class Consumer01 {
 
     //普通交换机名称
     public static final String NORMAL_EXCHANGE = "normal_exchange";
 
     //死信交换机名称
     public static final String DEAD_EXCHANGE = "dead_exchange";
 
     //普通队列名称
     public static final String NORMAL_QUEUE = "normal_queue";
 
     //死信队列名称
     public static final String DEAD_QUEUE = "dead_queue";
 
     public static void main(String[] args) throws  Exception{
         Channel channel = RabbitMqUtils.getChannel();
 
         //声明死信和普通的交换机类型为direct
         channel.exchangeDeclare(NORMAL_EXCHANGE, BuiltinExchangeType.DIRECT);
         channel.exchangeDeclare(DEAD_EXCHANGE, BuiltinExchangeType.DIRECT);
 
         //声明普通队列
         HashMap<String, Object> arguments = new HashMap<>();
 
         //过期时间
         arguments.put("x-message-ttl",1000);
         //正常队列设置死信队列
         arguments.put("x-dead-letter-exchange",DEAD_EXCHANGE);
         //设置死信RoutingKey
         arguments.put("x-dead-letter-routing-key","lisi");

     //声明死信和普通队列
     channel.queueDeclare(NORMAL_QUEUE,false,false,false,arguments);
     channel.queueDeclare(DEAD_QUEUE,false,false,false,null);

     //绑定普通的交换机与普通的队列
     channel.queueBind(NORMAL_QUEUE,NORMAL_EXCHANGE,"zhangsan");
     //绑定死信的交换机与死信的队列
     channel.queueBind(DEAD_QUEUE,DEAD_EXCHANGE,"lisi");
     System.out.println("等待接收消息......");

     DeliverCallback deliverCallback = (consumerTag,message) -> {
         System.out.println("Consumer01接收的消息是：" + new String(message.getBody()));
     };

     channel.basicConsume(NORMAL_QUEUE,true,deliverCallback,consumerTag->{});
 }
}

```
> 效果：启动生产者后，10条消息被传送到NORMAL_QUEUE，然后被传送到DEAD_QUEUE，此时启动消费者02，消息全被接收。

![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670374476561-793dfe3c-c35c-4304-89f7-94442cfcf776.png#averageHue=%23f5f0ef&clientId=ub616990b-7e68-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=456&id=uef85718e&margin=%5Bobject%20Object%5D&name=image.png&originHeight=912&originWidth=1603&originalType=binary&ratio=1&rotation=0&showTitle=false&size=412192&status=done&style=none&taskId=u1e314fac-06b0-43ff-8802-ce361e470bd&title=&width=801.5)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670374496613-dd57af63-3da1-4c25-b85a-e09b47ee2cfe.png#averageHue=%23f6f4f2&clientId=ub616990b-7e68-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=340&id=u7740f5a7&margin=%5Bobject%20Object%5D&name=image.png&originHeight=679&originWidth=1590&originalType=binary&ratio=1&rotation=0&showTitle=false&size=440373&status=done&style=none&taskId=ue6eda7ec-0e56-4d59-b004-bad009d48ee&title=&width=795)
### **6.3.3. 队列达到最大长度**
#### 1. 消息生产者代码去掉 TTL 属性
```
public class Producer {
    private static final String NORMAL_EXCHANGE = "normal_exchange";
    public static void main(String[] argv) throws Exception {
        try (Channel channel = RabbitMqUtils.getChannel())
        { channel.exchangeDeclare(NORMAL_EXCHANGE, BuiltinExchangeType.DIRECT);
//该信息是用作演示队列个数限制
            for (int i = 1; i <11 ; i++) {
                String message="info"+i;
                channel.basicPublish(NORMAL_EXCHANGE,"zhangsan",null, message.getBytes());
                System.out.println("生产者发送消息:"+message);
            }
        }
    }
}
```
#### 2. C1 消费者修改以下代码(**启动之后关闭该消费者 模拟其接收不到消息**) ![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670374626760-1e641ae5-33db-4719-b0d1-79172e2cf085.png#averageHue=%23bfe2c3&clientId=ub616990b-7e68-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=214&id=u931939ea&margin=%5Bobject%20Object%5D&name=image.png&originHeight=427&originWidth=1686&originalType=binary&ratio=1&rotation=0&showTitle=false&size=610238&status=done&style=none&taskId=ub90e5725-8cd2-4eaa-bf4a-965b5518941&title=&width=843)
**注意此时需要把原先队列删除 因为参数改变了**
#### 3. C2 消费者代码不变(启动 C2 消费者) 
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670374827684-5fe7dbee-0f47-42c1-b3d0-85baefd694b8.png#averageHue=%23f0eeec&clientId=ub616990b-7e68-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=348&id=u3361a35b&margin=%5Bobject%20Object%5D&name=image.png&originHeight=627&originWidth=1659&originalType=binary&ratio=1&rotation=0&showTitle=false&size=400309&status=done&style=none&taskId=uccb46365-dbfc-4908-97d6-14466ebe4af&title=&width=921.6666910824958)
### **6.3.4. 消息被拒**
#### 1.消息生产者代码同上生产者一致 
#### 2.C1 消费者代码(**启动之后关闭该消费者模拟其接收不到消息**) 
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670374849204-7779baf1-e9c3-4ec7-95e1-df566d5ecae7.png#averageHue=%23c4eaca&clientId=ub616990b-7e68-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=354&id=u92ed5237&margin=%5Bobject%20Object%5D&name=image.png&originHeight=637&originWidth=1588&originalType=binary&ratio=1&rotation=0&showTitle=false&size=211856&status=done&style=none&taskId=ufec9f9d6-5723-4037-828b-8f7f48fe2ae&title=&width=882.2222455931304)
```
public class Consumer01 {
    //普通交换机名称
    private static final String NORMAL_EXCHANGE = "normal_exchange";
    //死信交换机名称
    private static final String DEAD_EXCHANGE = "dead_exchange";
    public static void main(String[] argv) throws Exception {
        Channel channel = RabbitUtils.getChannel();
//声明死信和普通交换机 类型为 direct
        channel.exchangeDeclare(NORMAL_EXCHANGE, BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(DEAD_EXCHANGE, BuiltinExchangeType.DIRECT);
//声明死信队列
        String deadQueue = "dead-queue";
        channel.queueDeclare(deadQueue, false, false, false, null);
//死信队列绑定死信交换机与 routingkey
        channel.queueBind(deadQueue, DEAD_EXCHANGE, "lisi");
        //正常队列绑定死信队列信息
        Map<String, Object> params = new HashMap<>();
//正常队列设置死信交换机 参数 key 是固定值
        params.put("x-dead-letter-exchange", DEAD_EXCHANGE);
//正常队列设置死信 routing-key 参数 key 是固定值
        params.put("x-dead-letter-routing-key", "lisi");
        String normalQueue = "normal-queue";
        channel.queueDeclare(normalQueue, false, false, false, params);
        channel.queueBind(normalQueue, NORMAL_EXCHANGE, "zhangsan");
        System.out.println("等待接收消息........... ");
        DeliverCallback deliverCallback = (consumerTag, delivery) ->
        {String message = new String(delivery.getBody(), "UTF-8");
            if(message.equals("info5")){
                System.out.println("Consumer01 接收到消息" + message + "并拒绝签收该消息");
//requeue 设置为 false 代表拒绝重新入队 该队列如果配置了死信交换机将发送到死信队列中
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            }else {
                System.out.println("Consumer01 接收到消息"+message);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        boolean autoAck = false;
        channel.basicConsume(normalQueue, autoAck, deliverCallback, consumerTag -> {
        });
    }
}
```
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670374932143-2f77ac84-aa78-4220-8877-b2b61a5aecc3.png#averageHue=%23f1f1f1&clientId=ub616990b-7e68-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=148&id=u5ce55d2c&margin=%5Bobject%20Object%5D&name=image.png&originHeight=266&originWidth=1645&originalType=binary&ratio=1&rotation=0&showTitle=false&size=256118&status=done&style=none&taskId=ue5d0db6a-806c-4f21-8656-2f72c82966e&title=&width=913.8889130986773)
#### 3.C2 消费者代码不变 
**启动消费者 1 然后再启动消费者 2 **
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670374963313-4a32d697-88dc-4682-90b1-f59aa181197c.png#averageHue=%23f8f6f4&clientId=ub616990b-7e68-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=568&id=u93f9cc6b&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1023&originWidth=1596&originalType=binary&ratio=1&rotation=0&showTitle=false&size=604335&status=done&style=none&taskId=u8460b2f4-7314-4035-a4d9-906a46eb3e5&title=&width=886.6666901553125)

# **4.1. 发布确认原理**
生产者将信道设置成 confirm 模式，一旦信道进入 confirm 模式，**所有在该信道上面发布的消 **
**息都将会被指派一个唯一的 ID**(从 1 开始)，一旦消息被投递到所有匹配的队列之后，broker 就会 
发送一个确认给生产者(包含消息的唯一 ID)，这就使得生产者知道消息已经正确到达目的队列了， 
如果消息和队列是可持久化的，那么确认消息会在将消息写入磁盘之后发出，broker 回传给生产 
者的确认消息中 delivery-tag 域包含了确认消息的序列号，此外 broker 也可以设置basic.ack 的 
multiple 域，表示到这个序列号之前的所有消息都已经得到了处理。 
confirm 模式最大的好处在于他是异步的，一旦发布一条消息，生产者应用程序就可以在等信道 
返回确认的同时继续发送下一条消息，当消息最终得到确认之后，生产者应用便可以通过回调方 
法来处理该确认消息，如果 RabbitMQ 因为自身内部错误导致消息丢失，就会发送一条 nack 消息， 
生产者应用程序同样可以在回调方法中处理该 nack 消息。
# **4.2. 发布确认的策略**
## **4.2.1. 开启发布确认的方法**
发布确认默认是没有开启的，如果要开启需要调用方法 confirmSelect，每当你要想使用发布 
确认，都需要在 channel 上调用该方法
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670158169866-3fc20bdf-3d2a-4ce2-97bd-85f852fcf8a9.png#averageHue=%23323231&clientId=uf9a83c1d-f0a2-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=76&id=u34a1c8d4&margin=%5Bobject%20Object%5D&name=image.png&originHeight=151&originWidth=1151&originalType=binary&ratio=1&rotation=0&showTitle=false&size=92425&status=done&style=none&taskId=ue075acf1-757c-4ac3-b4a9-e5be9fcd2a4&title=&width=575.5)
## **4.2.2. 单个确认发布**
这是一种简单的确认方式，它是一种**同步确认发布**的方式，也就是发布一个消息之后只有它 
被确认发布，后续的消息才能继续发布,waitForConfirmsOrDie(long)这个方法只有在消息被确认 
的时候才返回，如果在指定时间范围内这个消息没有被确认那么它将抛出异常。 
这种确认方式有一个最大的缺点就是:**发布速度特别的慢，**因为如果没有确认发布的消息就会 
阻塞所有后续消息的发布，这种方式最多提供每秒不超过数百条发布消息的吞吐量。当然对于某 
些应用程序来说这可能已经足够了。
```
    /*
 * 发布确认模式，
 * 1、单个确认
 * 2、批量确认
 * 3、异步批量确认
 * */
 public class ComfirmMessage {
 
     // 批量发消息的个数
     public static final int MESSAGE_COUNT = 1000;
 
     public static void main(String[] args) throws Exception {
         // 1、单个确认
         // 发布1000个单独确认消息，耗时567ms
         ComfirmMessage.publishMessageIndividually();
 
     }
 
     public static void publishMessageIndividually() throws Exception {
         Channel channel = RabbitMqUtils.getChannel();
         String queueName = UUID.randomUUID().toString();
         channel.queueDeclare(queueName,false,false,false,null);

     // 开启发布确认
     channel.confirmSelect();
     // 开始时间
     long begin = System.currentTimeMillis();

     // 批量发消息
     for (int i = 0; i < MESSAGE_COUNT; i++) {
         String message = i + "";
         channel.basicPublish("",queueName,null,message.getBytes(StandardCharsets.UTF_8));
         // 单个消息马上进行发布确认
         boolean flag = channel.waitForConfirms();
         if (flag){
             System.out.println("消息发送成功");
         }
     }

     // 结束时间
     long end = System.currentTimeMillis();
     System.out.println("发布"+MESSAGE_COUNT+"个单独确认消息，耗时"+ (end - begin) + "ms");
 }
}

```
## **4.2.3. 批量确认发布**
上面那种方式非常慢，与单个等待确认消息相比，先发布一批消息然后一起确认可以极大地 
提高吞吐量，当然这种方式的缺点就是:当发生故障导致发布出现问题时，不知道是哪个消息出现 
问题了，我们必须将整个批处理保存在内存中，以记录重要的信息而后重新发布消息。当然这种 
方案仍然是同步的，也一样阻塞消息的发布。
```
/*
* 发布确认模式，
* 1、单个确认
* 2、批量确认
* 3、异步批量确认
* */
public class ComfirmMessage {

    // 批量发消息的个数
    public static final int MESSAGE_COUNT = 1000;

    public static void main(String[] args) throws Exception {
        //2、批量确认
        // 发布1000个批量确认消息，耗时37ms
        ComfirmMessage.publishMessageBatch();
    }

    public static void publishMessageBatch() throws Exception{
        Channel channel = RabbitMqUtils.getChannel();
        String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName,false,false,false,null);

        // 开启发布确认
        channel.confirmSelect();
        // 开始时间
        long begin = System.currentTimeMillis();

        // 批量确认消息大小
        int batchSize = 1000;

        // 批量发送 批量确认
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String message = i + "";
            channel.basicPublish("",queueName,null,message.getBytes(StandardCharsets.UTF_8));

            // 判断达到100条消息的时候，批量确认一次
            if (i%batchSize == 0){
                // 确认发布
                channel.waitForConfirms();
            }
        }

        // 结束时间
        long end = System.currentTimeMillis();
        System.out.println("发布"+MESSAGE_COUNT+"个批量确认消息，耗时"+ (end - begin) + "ms");
    }
}

```
## 4.2.4.异步发布确认
异步确认虽然编程逻辑比上两个要复杂，但是性价比最高，无论是可靠性还是效率都没得说，他是利用回调函数来达到消息可靠性传递的,这个中间件也是通过函数回调来保证是否投递成功，下面就让我们来详细讲解异步确认是怎么实现的。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670245215200-0e1f1830-0d65-4c6b-84bb-fb601572188a.png#averageHue=%23f7f7f7&clientId=u2b7f8c98-2e18-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=427&id=u4cd3797f&margin=%5Bobject%20Object%5D&name=image.png&originHeight=854&originWidth=1896&originalType=binary&ratio=1&rotation=0&showTitle=false&size=416267&status=done&style=none&taskId=u28b7e725-e427-4098-b715-34dd92c42c8&title=&width=948)
```
/*
* 发布确认模式，
* 1、单个确认
* 2、批量确认
* 3、异步批量确认
* */
public class ComfirmMessage {

    // 批量发消息的个数
    public static final int MESSAGE_COUNT = 1000;

    public static void main(String[] args) throws Exception {
        //3、异步批量确认
        // 发布1000个异步确认消息，耗时36ms
        ComfirmMessage.publicMessageAsync();

    }

    public static void publicMessageAsync() throws Exception{
        Channel channel = RabbitMqUtils.getChannel();
        String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName,false,false,false,null);

        // 开启发布确认
        channel.confirmSelect();
        // 开始时间
        long begin = System.currentTimeMillis();

        // 消息确认成功回调函数
        ConfirmCallback ackCallback = (deliveryTag,multiply) -> {
            System.out.println("确认的消息："+deliveryTag);
        };

        // 消息确认失败回调函数
        /*
        * 参数1：消息的标记
        * 参数2：是否为批量确认
        * */
        ConfirmCallback nackCallback = (deliveryTag,multiply) -> {
            System.out.println("未确认的消息："+deliveryTag);
        };

        // 准备消息的监听器，监听哪些消息成功，哪些消息失败
        /*
        * 参数1：监听哪些消息成功
        * 参数2：监听哪些消息失败
        * */
        channel.addConfirmListener(ackCallback,nackCallback);

        // 批量发送消息
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String message = "消息" + i;
            channel.basicPublish("",queueName,null,message.getBytes(StandardCharsets.UTF_8));
        }

        // 结束时间
        long end = System.currentTimeMillis();
        System.out.println("发布"+MESSAGE_COUNT+"个异步确认消息，耗时"+ (end - begin) + "ms");
    }
}

```
如何处理异步未确认信息？
> 最好的解决方案就是把未确认的消息放到一个基于内存的能被发布线程访问的队列，比如说用ConcurrentLinkedQueue这个队列在confirm callbacks与发布线程之间进行消息的传递

```
public class ComfirmMessage {

    // 批量发消息的个数
    public static final int MESSAGE_COUNT = 1000;

    public static void main(String[] args) throws Exception {
        //3、异步批量确认
        // 发布1000个异步确认消息，耗时36ms
        ComfirmMessage.publicMessageAsync();

    }

    public static void publicMessageAsync() throws Exception{
        Channel channel = RabbitMqUtils.getChannel();
        String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName,false,false,false,null);

        // 开启发布确认
        channel.confirmSelect();

        /*
        * 线程安全有序的一个哈希表 适用于高并发的情况下
        * 1、轻松地将序号与消息进行关联
        * 2、轻松地批量删除，只要给到序号
        * 3、支持高并发
        * */
        ConcurrentSkipListMap<Long,String> outstandingConfirms = new ConcurrentSkipListMap<>();

        // 消息确认成功回调函数
        ConfirmCallback ackCallback = (deliveryTag,multiply) -> {
            // 删除到已经确认的消息，剩下的就是未确认的消息
            if(multiply){
                ConcurrentNavigableMap<Long, String> confiremed = outstandingConfirms.headMap(deliveryTag);
                confiremed.clear();
            }else {
                outstandingConfirms.remove(deliveryTag);
            }

            System.out.println("确认的消息："+deliveryTag);
        };

        // 消息确认失败回调函数
        /*
        * 参数1：消息的标记
        * 参数2：是否为批量确认
        * */
        ConfirmCallback nackCallback = (deliveryTag,multiply) -> {
            // 打印一下未确认的消息都有哪些
            String message = outstandingConfirms.get(deliveryTag);
            System.out.println("未确认的消息是：" + message +"未确认的消息tag：" + deliveryTag);
        };

        // 准备消息的监听器，监听哪些消息成功，哪些消息失败
        /*
        * 参数1：监听哪些消息成功
        * 参数2：监听哪些消息失败
        * */
        channel.addConfirmListener(ackCallback,nackCallback);

        // 开始时间
        long begin = System.currentTimeMillis();

        // 批量发送消息
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String message = "消息" + i;
            channel.basicPublish("",queueName,null,message.getBytes(StandardCharsets.UTF_8));

            // 此处记录下所有要发送的消息的总和
            outstandingConfirms.put(channel.getNextPublishSeqNo(),message);
        }



        // 结束时间
        long end = System.currentTimeMillis();
        System.out.println("发布"+MESSAGE_COUNT+"个异步确认消息，耗时"+ (end - begin) + "ms");
    }
}

```
## 三种发布确认速度对比
| **发布方式** | **特点** |
| --- | --- |
| 单独发布消息 | 同步等待确认，简单，但吞吐量非常有限 |
| 批量发布消息 | 批量同步等待确认，简单，合理的吞吐量，一旦出现问题但很难推断出是那条消息出现了问题。 |
| 异步处理 | 最佳性能和资源使用，在出现错误的情况下可以很好地控制，但是实现起来稍微难些 |


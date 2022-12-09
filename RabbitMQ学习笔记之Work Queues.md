> 工作队列(又称任务队列)的主要思想是避免立即执行资源密集型任务，而不得不等待它完成。 
> 相反我们安排任务在之后执行。我们把任务封装为消息并将其发送到队列。在后台运行的工作进 
> 程将弹出任务并最终执行作业。当有多个工作线程时，这些工作线程将一起处理这些任务。 

## **3.1. 轮训分发消息**
在这个案例中我们会启动两个工作线程，一个消息发送线程，我们来看看他们两个工作线程 
是如何工作的。 
### **3.1.1. 抽取工具类**
```
/*
* 此类为连接工厂创建信道的工具类
* */
public class RabbitMqUtils {
     // 得到一个连接的channel
    public static Channel getChannel() throws IOException, TimeoutException {
        // 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.163.128");
        factory.setUsername("admin");
        factory.setPassword("123");
        Connection connection = factory.newConnection();
        com.rabbitmq.client.Channel channel = connection.createChannel();
        return channel;
    }
}

```
### **3.1.2. 启动两个工作线程**
```
public class Worker01 {

    // 队列名称
    public static final String QUEUE_NAME = "hello";

    // 接受消息
    public static void main(String[] args) throws IOException, TimeoutException {
        Channel channel = RabbitMqUtils.getChannel();

        // 接受消息参数
        DeliverCallback deliverCallback = (consumerTag,message) -> {
            System.out.println("接受到的消息："+message.getBody());
        };

        // 取消消费参数
        CancelCallback cancelCallback = consumerTag -> {
            System.out.println(consumerTag+"消费者取消消费借口回调逻辑");
        };

        // 消息的接受
        channel.basicConsume(QUEUE_NAME,true,deliverCallback,cancelCallback);
    }
}

```
#### 设置可以轮询运行
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670142108258-208426b2-355f-49b3-a859-96fe42bc4abe.png#averageHue=%233e4347&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=555&id=u9e60916c&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1109&originWidth=901&originalType=binary&ratio=1&rotation=0&showTitle=false&size=127884&status=done&style=none&taskId=u60be94f5-0dec-4804-9a72-62d1736cab6&title=&width=450.5)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670142147144-c62f5dd5-4a72-4b8e-ad57-e3893498338d.png#averageHue=%23717051&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=170&id=uc6db25e4&margin=%5Bobject%20Object%5D&name=image.png&originHeight=340&originWidth=1606&originalType=binary&ratio=1&rotation=0&showTitle=false&size=492059&status=done&style=none&taskId=u6cf15d9f-c3d9-4766-8133-52f3cc121d6&title=&width=803)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670142157891-9f366ad4-229f-4b06-9fe5-0d8d832dcc6b.png#averageHue=%23344532&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=204&id=uf97c3631&margin=%5Bobject%20Object%5D&name=image.png&originHeight=407&originWidth=1503&originalType=binary&ratio=1&rotation=0&showTitle=false&size=553503&status=done&style=none&taskId=uaf7a7e4a-7f23-4541-8aee-f6519370a86&title=&width=751.5)
### **3.1.3. 启动一个发送线程**
```
public class Task01 {
    // 队列名称
    public static final String QUEUE_NAME = "hello";

    // 发送大量消息
    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMqUtils.getChannel();

        // 队列的声明
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);

        // 从控制台中输入消息
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()){
            String message = scanner.next();
            channel.basicPublish("",QUEUE_NAME,null,message.getBytes(StandardCharsets.UTF_8));
            System.out.println("发送消息完成："+ message);

        }
    }
}

```
### **3.1.4. 结果展示**
通过程序执行发现生产者总共发送 4 个消息，消费者 1 和消费者 2 分别分得两个消息，并且 
是按照有序的一个接收一次消息
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670142457521-2aff512d-8a80-4687-a3ac-e2ec7a2ac604.png#averageHue=%2374704c&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=442&id=u78b0a849&margin=%5Bobject%20Object%5D&name=image.png&originHeight=884&originWidth=1595&originalType=binary&ratio=1&rotation=0&showTitle=false&size=1104983&status=done&style=none&taskId=u9a80a1d8-bc20-478b-86de-aa14a00e076&title=&width=797.5)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670142497331-d67617b5-2111-4ef9-a08b-9382569b31a9.png#averageHue=%23755d44&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=324&id=ue6bcc1f9&margin=%5Bobject%20Object%5D&name=image.png&originHeight=647&originWidth=1927&originalType=binary&ratio=1&rotation=0&showTitle=false&size=882825&status=done&style=none&taskId=ubfeaeded-0d8b-4ce7-9a48-25de91fb219&title=&width=963.5)![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670142505358-ad02e9b9-dc44-42d4-bc79-0f15d098e928.png#averageHue=%23744e42&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=263&id=ud395cf60&margin=%5Bobject%20Object%5D&name=image.png&originHeight=526&originWidth=1574&originalType=binary&ratio=1&rotation=0&showTitle=false&size=588726&status=done&style=none&taskId=u6d99b58a-a807-48ee-ae45-359478d0d12&title=&width=787)
## **3.2. 消息应答**
### **3.2.1. 概念**
消费者完成一个任务可能需要一段时间，如果其中一个消费者处理一个长的任务并仅只完成 
了部分突然它挂掉了，会发生什么情况。RabbitMQ 一旦向消费者传递了一条消息，便立即将该消 
息标记为删除。在这种情况下，突然有个消费者挂掉了，我们将丢失正在处理的消息。以及后续 
发送给该消费这的消息，因为它无法接收到。 
为了保证消息在发送过程中不丢失，rabbitmq 引入消息应答机制，消息应答就是:**消费者在接收 **
**到消息并且处理该消息之后，告诉 rabbitmq 它已经处理了，rabbitmq 可以把该消息删除了。**
### **3.2.2. 自动应答**
消息发送后立即被认为已经传送成功，这种模式需要在**高吞吐量和数据传输安全性方面做权 **
**衡**,因为这种模式如果消息在接收到之前，消费者那边出现连接或者 channel 关闭，那么消息就丢失 
了,当然另一方面这种模式消费者那边可以传递过载的消息，**没有对传递的消息数量进行限制**，当 
然这样有可能使得消费者这边由于接收太多还来不及处理的消息，导致这些消息的积压，最终使 
得内存耗尽，最终这些消费者线程被操作系统杀死，**所以这种模式仅适用在消费者可以高效并以 **
**某种速率能够处理这些消息的情况下使用**。
### **3.2.3. 消息应答的方法**
**A**.Channel.basicAck(用于肯定确认) 
RabbitMQ 已知道该消息并且成功的处理消息，可以将其丢弃了 
**B**.Channel.basicNack(用于否定确认) 
**C**.Channel.basicReject(用于否定确认) 
与 Channel.basicNack 相比少一个参数 
 不处理该消息了直接拒绝，可以将其丢弃了 
### **3.2.4. Multiple 的解释**
> **手动应答的好处是可以批量应答并且减少网络拥堵**

![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670142807660-b97d3991-d8ea-46b3-8bfd-a50d69c1978b.png#averageHue=%23302e2a&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=121&id=u4d279d13&margin=%5Bobject%20Object%5D&name=image.png&originHeight=242&originWidth=1272&originalType=binary&ratio=1&rotation=0&showTitle=false&size=206728&status=done&style=none&taskId=ua47964cf-a746-4c48-8776-c6405c255cf&title=&width=636)
> multiple 的 true 和 false 代表不同意思 
> true 代表批量应答 channel 上未应答的消息 
>  比如说 channel 上有传送 tag 的消息 5,6,7,8 当前 tag 是8 那么此时 
>  5-8 的这些还未应答的消息都会被确认收到消息应答 
> false 同上面相比 
>  只会应答 tag=8 的消息 5,6,7 这三个消息依然不会被确认收到消息应答

![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670142842230-509debfd-b4e0-4695-8ded-a2c42e59f9ea.png#averageHue=%23f6f6f5&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=567&id=u4b92e824&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1133&originWidth=1370&originalType=binary&ratio=1&rotation=0&showTitle=false&size=180104&status=done&style=none&taskId=uc7bfb99f-96ca-47ba-9d86-7b897a21607&title=&width=685)
### **3.2.5. 消息自动重新入队 **
如果消费者由于某些原因失去连接(其通道已关闭，连接已关闭或 TCP 连接丢失)，导致消息 
未发送 ACK 确认，RabbitMQ 将了解到消息未完全处理，并将对其重新排队。如果此时其他消费者 
可以处理，它将很快将其重新分发给另一个消费者。这样，即使某个消费者偶尔死亡，也可以确 
保不会丢失任何消息
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670143088015-dd1b1085-ff90-4a6f-877c-e429bed61cbf.png#averageHue=%23f0e1e1&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=351&id=uff5aa6b5&margin=%5Bobject%20Object%5D&name=image.png&originHeight=701&originWidth=1637&originalType=binary&ratio=1&rotation=0&showTitle=false&size=218446&status=done&style=none&taskId=u6369b343-1fe7-4beb-a0be-ae3b9f8a83c&title=&width=818.5)
### **3.2.6. 消息手动应答代码**
默认消息采用的是自动应答，所以我们要想实现消息消费过程中不丢失，需要把自动应答改 
为手动应答，消费者在上面代码的基础上增加下面画红色部分代码。
#### 生产者
```
    /*
 * 消息在手动应答时是不丢失、放回队列中重新消费
 * */
 public class Task2 {
 
     // 队列名称
     public static final String TASK_QUEUE_NAME = "ack_queue";
 
     public static void main(String[] args) throws IOException, TimeoutException {
         Channel channel = RabbitMqUtils.getChannel();
 
         // 声明队列
         channel.queueDeclare(TASK_QUEUE_NAME,false,false,false,null);
 
         Scanner scanner = new Scanner(System.in);
         while (scanner.hasNext()){
             String message = scanner.next();
             channel.basicPublish("",TASK_QUEUE_NAME,null,message.getBytes(StandardCharsets.UTF_8));
             System.out.println("生产者发出消息："+message);
         }
 
     }
 }

```
#### 消费者
```
    /*
 * 消费者
 * */
 public class Worker03 {
     // 队列名称
     public static final String TASK_QUEUE_NAME = "ack_queue";
 
     public static void main(String[] args) throws IOException, TimeoutException {
         Channel channel = RabbitMqUtils.getChannel();
         System.out.println("C1等待接受消息处理时间较短");

     DeliverCallback deliverCallback = (consumerTag,message) -> {
         // 沉睡一秒
         SleepUtils.sleep(1);
         System.out.println("接受到的消息是:"+new String(message.getBody()));

         //进行手动应答
         /*
         * 参数1：消息的标记  tag
         * 参数2：是否批量应答，false：不批量应答 true：批量
         * */
         channel.basicAck(message.getEnvelope().getDeliveryTag(),false);
     };


     // 采用手动应答
     boolean autoAck = false;
     channel.basicConsume(TASK_QUEUE_NAME,autoAck,deliverCallback,(consumerTag) -> {
         System.out.println(consumerTag+"消费者取消消费接口回调逻辑");
     });
 }
}

```
#### 工具类
```
public class SleepUtils {
    public static void sleep(int second){
        try {
            Thread.sleep(1000*second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.currentThread().interrupt();
    }
}
```
### **3.2.7. 手动应答效果演示**
正常情况下消息发送方发送两个消息 C1 和 C2 分别接收到消息并进行处理
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670144271402-50d86759-46a3-4caa-b817-3464edf01412.png#averageHue=%23f1f0f0&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=235&id=u59177c42&margin=%5Bobject%20Object%5D&name=image.png&originHeight=470&originWidth=1549&originalType=binary&ratio=1&rotation=0&showTitle=false&size=134127&status=done&style=none&taskId=uc11fa2eb-4d8d-4bc1-b385-37f427cad08&title=&width=774.5)
在发送者发送消息 dd，发出消息之后的把 C2 消费者停掉，按理说该 C2 来处理该消息，但是 
由于它处理时间较长，在还未处理完，也就是说 C2 还没有执行 ack 代码的时候，C2 被停掉了， 
此时会看到消息被 C1 接收到了，说明消息 dd 被重新入队，然后分配给能处理消息的 C1 处理了
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670144288346-3ed014d2-ca2e-4fd0-b7f7-95c345d188c9.png#averageHue=%23f3f0ef&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=204&id=ua92e2ef2&margin=%5Bobject%20Object%5D&name=image.png&originHeight=408&originWidth=1742&originalType=binary&ratio=1&rotation=0&showTitle=false&size=166584&status=done&style=none&taskId=u9cfac4b1-f377-43ef-b1ba-8b969d60039&title=&width=871)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670144305234-a0f971a5-6120-41aa-aef0-17b70f3b3e91.png#averageHue=%23f4f3f2&clientId=u07a00d59-a3e3-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=398&id=u883d3bb9&margin=%5Bobject%20Object%5D&name=image.png&originHeight=795&originWidth=1688&originalType=binary&ratio=1&rotation=0&showTitle=false&size=262168&status=done&style=none&taskId=udd01d595-4375-4155-bb95-3e7fb825ed1&title=&width=844)
## **3.3. RabbitMQ 持久化 **
### **3.3.1.概念**
刚刚我们已经看到了如何处理任务不丢失的情况，但是如何保障当 RabbitMQ 服务停掉以后消 
息生产者发送过来的消息不丢失。默认情况下 RabbitMQ 退出或由于某种原因崩溃时，它忽视队列 
和消息，除非告知它不要这样做。确保消息不会丢失需要做两件事：**我们需要将队列和消息都标 **
**记为持久化**。 
### **3.3.2. 队列如何实现持久化**
之前我们创建的队列都是非持久化的，rabbitmq 如果重启的化，该队列就会被删除掉，如果 
 要队列实现持久化 需要在声明队列的时候把 durable 参数设置为持久化
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670153940165-e1a22660-b4ba-4721-acd7-da4b8eb15723.png#averageHue=%23c4e6c8&clientId=ua79463b9-caa4-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=57&id=u484ff9b7&margin=%5Bobject%20Object%5D&name=image.png&originHeight=113&originWidth=1705&originalType=binary&ratio=1&rotation=0&showTitle=false&size=159084&status=done&style=none&taskId=uc457fd23-dff1-4b38-82dc-87c3b2acbc8&title=&width=852.5)
但是需要注意的就是如果之前声明的队列不是持久化的，需要把原先队列先删除，或者重新 
创建一个持久化的队列，不然就会出现错误
> 以下为控制台中持久化与非持久化队列的 UI 显示区**、这个时候即使重启 rabbitmq 队列也依然存在**

![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670153973764-d7be0ab4-ec40-4ddc-8941-95b25dcec93e.png#averageHue=%23f1efee&clientId=ua79463b9-caa4-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=185&id=uea7828c4&margin=%5Bobject%20Object%5D&name=image.png&originHeight=369&originWidth=1640&originalType=binary&ratio=1&rotation=0&showTitle=false&size=167337&status=done&style=none&taskId=u75b03306-0230-44af-bbea-8032fab7552&title=&width=820)
### **3.3.3. 消息实现持久化**
要想让消息实现持久化需要在消息生产者修改代码，MessageProperties.PERSISTENT_TEXT_PLAIN 添 
加这个属性。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670154015168-18cfd4ff-f917-4a18-a10f-83872e698fc7.png#averageHue=%23c9e5cd&clientId=ua79463b9-caa4-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=73&id=u6738d753&margin=%5Bobject%20Object%5D&name=image.png&originHeight=146&originWidth=1669&originalType=binary&ratio=1&rotation=0&showTitle=false&size=241351&status=done&style=none&taskId=uea24848b-68af-4aa1-b671-5839b182bf6&title=&width=834.5)
将消息标记为持久化并不能完全保证不会丢失消息。尽管它告诉 RabbitMQ 将消息保存到磁盘，但是 
这里依然存在当消息刚准备存储在磁盘的时候 但是还没有存储完，消息还在缓存的一个间隔点。此时并没 
有真正写入磁盘。持久性保证并不强，但是对于我们的简单任务队列而言，这已经绰绰有余了。如果需要 
更强有力的持久化策略，参考后边课件发布确认章节。 
### **3.3.4. 不公平分发 **
在最开始的时候我们学习到 RabbitMQ 分发消息采用的轮训分发，但是在某种场景下这种策略并不是 
很好，比方说有两个消费者在处理任务，其中有个消费者 1 处理任务的速度非常快，而另外一个消费者 2 
处理速度却很慢，这个时候我们还是采用轮训分发的化就会到这处理速度快的这个消费者很大一部分时间 
处于空闲状态，而处理慢的那个消费者一直在干活，这种分配方式在这种情况下其实就不太好，但是 
RabbitMQ 并不知道这种情况它依然很公平的进行分发。 
为了避免这种情况，我们可以设置参数 channel.basicQos(1);
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670154039438-a7185ced-f847-4c1e-9ab2-8e484787f003.png#averageHue=%23302f2f&clientId=ua79463b9-caa4-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=106&id=ue8f4f3b7&margin=%5Bobject%20Object%5D&name=image.png&originHeight=212&originWidth=1094&originalType=binary&ratio=1&rotation=0&showTitle=false&size=99631&status=done&style=none&taskId=u42fde162-3ef0-4428-9836-798f4b38aef&title=&width=547)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670154053689-22ece162-c67c-42a1-9428-c97cc648a8bc.png#averageHue=%23f7f7f7&clientId=ua79463b9-caa4-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=384&id=u197b2f52&margin=%5Bobject%20Object%5D&name=image.png&originHeight=767&originWidth=1683&originalType=binary&ratio=1&rotation=0&showTitle=false&size=498146&status=done&style=none&taskId=u9a88666c-d6d6-4066-a059-533f0012cc3&title=&width=841.5)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670154062768-d2a9c52a-5c72-4598-b949-493ca9a7f827.png#averageHue=%23f6eceb&clientId=ua79463b9-caa4-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=239&id=ue932765d&margin=%5Bobject%20Object%5D&name=image.png&originHeight=477&originWidth=1546&originalType=binary&ratio=1&rotation=0&showTitle=false&size=189033&status=done&style=none&taskId=ubdecc604-c969-443c-9f24-e2713fcca1d&title=&width=773)
意思就是如果这个任务我还没有处理完或者我还没有应答你，你先别分配给我，我目前只能处理一个 
任务，然后 rabbitmq 就会把该任务分配给没有那么忙的那个空闲消费者，当然如果所有的消费者都没有完 
成手上任务，队列还在不停的添加新任务，队列有可能就会遇到队列被撑满的情况，这个时候就只能添加 
新的 worker 或者改变其他存储任务的策略。 
### **3.3.5. 预取值**
本身消息的发送就是异步发送的，所以在任何时候，channel 上肯定不止只有一个消息另外来自消费 
者的手动确认本质上也是异步的。因此这里就存在一个未确认的消息缓冲区，因此希望开发人员能**限制此 **
**缓冲区的大小，以避免缓冲区里面无限制的未确认消息问题**。这个时候就可以通过使用 basic.qos 方法设 
置“预取计数”值来完成的。**该值定义通道上允许的未确认消息的最大数量**。一旦数量达到配置的数量， 
RabbitMQ 将停止在通道上传递更多消息，除非至少有一个未处理的消息被确认，例如，假设在通道上有 
未确认的消息 5、6、7，8，并且通道的预取计数设置为 4，此时RabbitMQ 将不会在该通道上再传递任何 消息，除非至少有一个未应答的消息被 ack。比方说 tag=6 这个消息刚刚被确认 ACK，RabbitMQ 将会感知 这个情况到并再发送一条消息。消息应答和 QoS 预取值对用户吞吐量有重大影响。通常，增加预取将提高 向消费者传递消息的速度。**虽然自动应答传输消息速率是最佳的，但是，在这种情况下已传递但尚未处理的消息的数量也会增加，从而增加了消费者的 RAM 消耗**(随机存取存储器)应该小心使用具有无限预处理 的自动确认模式或手动确认模式，消费者消费了大量的消息如果没有确认的话，会导致消费者连接节点的 内存消耗变大，所以找到合适的预取值是一个反复试验的过程，不同的负载该值取值也不同 100 到 300 范 围内的值通常可提供最佳的吞吐量，并且不会给消费者带来太大的风险。预取值为 1 是最保守的。当然这 将使吞吐量变得很低，特别是消费者连接延迟很严重的情况下，特别是在消费者连接等待时间较长的环境 中。对于大多数应用来说，稍微高一点的值将是最佳的.
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670154100776-01dff390-4dad-4c57-af28-bc46148df718.png#averageHue=%23f5f4f3&clientId=ua79463b9-caa4-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=270&id=u65ae411b&margin=%5Bobject%20Object%5D&name=image.png&originHeight=540&originWidth=1633&originalType=binary&ratio=1&rotation=0&showTitle=false&size=144295&status=done&style=none&taskId=u609f34a5-7014-4037-b98d-cf1c59a29d6&title=&width=816.5)

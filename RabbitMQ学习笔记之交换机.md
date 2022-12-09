# **5. 交换机**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670320792843-5997d95b-848a-43b3-9cb0-3631338030c2.png#averageHue=%23f4f4f4&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=247&id=u082b02e2&margin=%5Bobject%20Object%5D&name=image.png&originHeight=493&originWidth=1877&originalType=binary&ratio=1&rotation=0&showTitle=false&size=199192&status=done&style=none&taskId=ua4d8b7e2-4bd3-48bc-82be-a0563c3d3f3&title=&width=938.5)
## 5.1 交换机简介
> 在上一节中，我们创建了一个工作队列。我们假设的是工作队列背后，每个任务都恰好交付给一个消 
> 费者(工作进程)。在这一部分中，我们将做一些完全不同的事情-我们将消息传达给多个消费者。这种模式 
> 称为 ”发布/订阅”. 
> 为了说明这种模式，我们将构建一个简单的日志系统。它将由两个程序组成:第一个程序将发出日志消 
> 息，第二个程序是消费者。其中我们会启动两个消费者，其中一个消费者接收到消息后把日志存储在磁盘另外一个消费者接收到消息后把消息打印在屏幕上，事实上第一个程序发出的日志消息将广播给所有消费 者

### **5.1.1. Exchanges 概念 **
RabbitMQ 消息传递模型的核心思想是: **生产者生产的消息从不会直接发送到队列**。实际上，通常生产 
者甚至都不知道这些消息传递传递到了哪些队列中。 
相反，**生产者只能将消息发送到交换机(exchange)**，交换机工作的内容非常简单，一方面它接收来 
自生产者的消息，另一方面将它们推入队列。交换机必须确切知道如何处理收到的消息。是应该把这些消 
息放到特定队列还是说把他们到许多队列中还是说应该丢弃它们。这就的由交换机的类型来决定。![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670320751783-55c383fa-87c9-4456-921a-e8c8f14b5c57.png#averageHue=%23f0c1bd&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=206&id=ue283dcd1&margin=%5Bobject%20Object%5D&name=image.png&originHeight=411&originWidth=1120&originalType=binary&ratio=1&rotation=0&showTitle=false&size=115006&status=done&style=none&taskId=u048307a1-f23f-44a2-8b6f-29efe9b2638&title=&width=560)
### **5.1.2. Exchanges 的类型**
总共有以下类型： 
直接(direct), 主题(topic) ,标题(headers) , 扇出(fanout) 
### **5.1.3. 无名exchange **
在本教程的前面部分我们对 exchange 一无所知，但仍然能够将消息发送到队列。之前能实现的 
原因是因为我们使用的是默认交换，我们通过空字符串(“”)进行标识。
```
channel.basiPublish("","hello",null,message.getBytes())；

```
第一个参数是交换机的名称。空字符串表示默认或无名称交换机：消息能路由发送到队列中其实 
是由 routingKey(bindingkey)绑定 key 指定的，如果它存在的话
## **5.2. 临时队列**
之前的章节我们使用的是具有特定名称的队列(还记得 hello 和 ack_queue 吗？)。队列的名称我们 
来说至关重要-我们需要指定我们的消费者去消费哪个队列的消息。 
每当我们连接到 Rabbit 时，我们都需要一个全新的空队列，为此我们可以创建一个具有**随机名称 **
**的队列**，或者能让服务器为我们选择一个随机队列名称那就更好了。其次**一旦我们断开了消费者的连 **
**接，队列将被自动删除。 **
创建临时队列的方式如下: 
> String queueName = channel.queueDeclare().getQueue(); 

创建出来之后长成这样![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670320924099-f1414afb-f8b2-4f86-9da2-abff5396aed8.png#averageHue=%23f5f0ed&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=194&id=u2d209f9c&margin=%5Bobject%20Object%5D&name=image.png&originHeight=388&originWidth=1621&originalType=binary&ratio=1&rotation=0&showTitle=false&size=125490&status=done&style=none&taskId=ue42700e6-1047-4877-bf87-cb404fdd6a3&title=&width=810.5)
## **5.3. 绑定(bindings) **
什么是 bingding 呢，binding 其实是 exchange 和 queue 之间的桥梁，它告诉我们 exchange 和那个队 
列进行了绑定关系。比如说下面这张图告诉我们的就是 X 与 Q1 和 Q2 进行了绑定![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670320951109-296bd756-cdf2-4e59-b6f8-a4bb4ebb300e.png#averageHue=%23f2d2cf&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=162&id=u588e8230&margin=%5Bobject%20Object%5D&name=image.png&originHeight=323&originWidth=808&originalType=binary&ratio=1&rotation=0&showTitle=false&size=115032&status=done&style=none&taskId=u3fb9c1e3-5342-4384-b3a4-d21ea7701dc&title=&width=404)
## **5.4. Fanout **
### **5.4.1. Fanout 介绍 **
Fanout 这种类型非常简单。正如从名称中猜到的那样，它是将接收到的所有消息**广播**到它知道的 
所有队列中。系统中默认有些 exchange 类型![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670320992707-b7ed3ae6-48d6-4c35-9777-c8f864021a6f.png#averageHue=%23f4f2f1&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=396&id=ufc09078a&margin=%5Bobject%20Object%5D&name=image.png&originHeight=791&originWidth=1167&originalType=binary&ratio=1&rotation=0&showTitle=false&size=166575&status=done&style=none&taskId=u77139b1c-1a34-4221-b53e-639515ad225&title=&width=583.5)
### **5.4.2. Fanout 实战 **![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670321019320-d9987321-1fcd-4ef1-8f26-7056f577a729.png#averageHue=%23e6e4e0&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=198&id=u0d01a957&margin=%5Bobject%20Object%5D&name=image.png&originHeight=396&originWidth=1667&originalType=binary&ratio=1&rotation=0&showTitle=false&size=379192&status=done&style=none&taskId=u80558e19-c6e1-4c02-8ac2-d718164d7bc&title=&width=833.5)
Logs 和临时队列的绑定关系如下图
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670321037474-a7fd63bb-bfe1-4692-b1ef-adda138013b9.png#averageHue=%23f5f4f4&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=256&id=uf40ecec2&margin=%5Bobject%20Object%5D&name=image.png&originHeight=511&originWidth=1064&originalType=binary&ratio=1&rotation=0&showTitle=false&size=90130&status=done&style=none&taskId=u4fae65c0-8dd7-4e9d-93d8-15acc15abd4&title=&width=532)
> ReceiveLogs01 将接收到的消息打印在控制台
1.消费者

```
    /*
 * 消息接收
 * */
 public class ReceiveLogs01 {
 
     //交换机名称
     public static final String EXCHANGE_NAME = "logs";
 
     public static void main(String[] args) throws Exception{
         Channel channel = RabbitMqUtils.getChannel();

     //声明一个队列,名称随机，当消费者断开与队列的连接时，队列自动删除
     String queueName = channel.queueDeclare().getQueue();

     //绑定交换机与队列
     channel.queueBind(queueName,EXCHANGE_NAME,"");
     System.out.println("等待接受消息，把接受到的消息打印在屏幕上...");


     DeliverCallback deliverCallback = (consumerTag,message) -> {
         System.out.println("ReceiveLogs01控制台打印接受到的消息：" + new String(message.getBody()));
     };

     channel.basicConsume(queueName,true,deliverCallback,consumerTag -> {});
 }
}

```
```
 /*
 * 消息接收
 * */
 public class ReceiveLogs02 {
 
     //交换机名称
     public static final String EXCHANGE_NAME = "logs";
 
     public static void main(String[] args) throws Exception{
         Channel channel = RabbitMqUtils.getChannel();

     //声明一个队列,名称随机，当消费者断开与队列的连接时，队列自动删除
     String queueName = channel.queueDeclare().getQueue();

     //绑定交换机与队列
     channel.queueBind(queueName,EXCHANGE_NAME,"");
     System.out.println("等待接受消息，把接受到的消息打印在屏幕上...");


     DeliverCallback deliverCallback = (consumerTag,message) -> {
         System.out.println("ReceiveLogs02控制台打印接受到的消息：" + new String(message.getBody()));
     };

     channel.basicConsume(queueName,true,deliverCallback,consumerTag -> {});
 }
}

```
> 生产者

```
 /*
 *  发消息 交换机
 * */
 public class Emitlog {
     // 交换机的名称
     public  static  final String EXCHANGE_NAME = "logs";
 
     public static void main(String[] args) throws  Exception{
         Channel channel = RabbitMqUtils.getChannel();
         channel.exchangeDeclare(EXCHANGE_NAME,"fauout");

     Scanner scanner = new Scanner(System.in);
     while (scanner.hasNext()){
         String message = scanner.next();
         channel.basicPublish(EXCHANGE_NAME,"",null,message.getBytes(StandardCharsets.UTF_8));
         System.out.println("生产者发出的消息："+ message);
     }
 }
}

```
## **5.5. Direct exchange **
### **5.5.1. 回顾**
在上一节中，我们构建了一个简单的日志记录系统。我们能够向许多接收者广播日志消息。在本 
节我们将向其中添加一些特别的功能-比方说我们只让某个消费者订阅发布的部分消息。例如我们只把 
严重错误消息定向存储到日志文件(以节省磁盘空间)，同时仍然能够在控制台上打印所有日志消息。 
我们再次来回顾一下什么是 bindings，绑定是交换机和队列之间的桥梁关系。也可以这么理解： 
**队列只对它绑定的交换机的消息感兴趣**。绑定用参数：routingKey 来表示也可称该参数为 binding key， 
创建绑定我们用代码:channel.queueBind(queueName, EXCHANGE_NAME, "routingKey");**绑定之后的 **
**意义由其交换类型决定。 **
### **5.5.2. Direct exchange 介绍**
上一节中的我们的日志系统将所有消息广播给所有消费者，对此我们想做一些改变，例如我们希 
望将日志消息写入磁盘的程序仅接收严重错误(errros)，而不存储哪些警告(warning)或信息(info)日志 
消息避免浪费磁盘空间。Fanout 这种交换类型并不能给我们带来很大的灵活性-它只能进行无意识的 
广播，在这里我们将使用 direct 这种类型来进行替换，这种类型的工作方式是，消息只去到它绑定的 
routingKey 队列中去。![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670321167174-2ba6d287-8709-4b46-ad79-8bea4d30c370.png#averageHue=%23f4e0df&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=219&id=ub6624d2f&margin=%5Bobject%20Object%5D&name=image.png&originHeight=437&originWidth=1422&originalType=binary&ratio=1&rotation=0&showTitle=false&size=196009&status=done&style=none&taskId=uf9616658-213b-48b7-af6f-4db476dad63&title=&width=711)
在上面这张图中，我们可以看到 X 绑定了两个队列，绑定类型是 direct。队列Q1 绑定键为 orange， 
队列 Q2 绑定键有两个:一个绑定键为 black，另一个绑定键为 green. 
在这种绑定情况下，生产者发布消息到 exchange 上，绑定键为 orange 的消息会被发布到队列 
Q1。绑定键为 blackgreen 和的消息会被发布到队列 Q2，其他消息类型的消息将被丢弃。 
### **5.5.3. 多重绑定**![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670321195154-29c236fb-9b02-46fa-9361-c3175d90f24e.png#averageHue=%23f6e3e1&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=222&id=u08393cc9&margin=%5Bobject%20Object%5D&name=image.png&originHeight=444&originWidth=1224&originalType=binary&ratio=1&rotation=0&showTitle=false&size=156318&status=done&style=none&taskId=u72efdaa7-bf6b-48bd-ab88-2d43fa1b87d&title=&width=612)
当然如果 exchange 的绑定类型是direct，**但是它绑定的多个队列的 key 如果都相同**，在这种情 
况下虽然绑定类型是 direct **但是它表现的就和 fanout 有点类似了**，就跟广播差不多，如上图所示。
### **5.5.4. 实战**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670321235140-f03a55a2-35c1-4d37-96c3-8181d96be44d.png#averageHue=%23f5e8e7&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=298&id=u1c6bf92d&margin=%5Bobject%20Object%5D&name=image.png&originHeight=595&originWidth=1291&originalType=binary&ratio=1&rotation=0&showTitle=false&size=283727&status=done&style=none&taskId=uef81bf9f-67de-445b-95a4-f408c081e5a&title=&width=645.5)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670321236390-928f437e-de1f-41ce-aff2-00d33339283e.png#averageHue=%23f2f1f1&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=261&id=u4ce4f01f&margin=%5Bobject%20Object%5D&name=image.png&originHeight=521&originWidth=682&originalType=binary&ratio=1&rotation=0&showTitle=false&size=62853&status=done&style=none&taskId=u0c727818-b111-4a6c-9c82-4b61e73a6c6&title=&width=341)
> 生产者

```
public class DirectLogs {
 // 交换机的名称
 public  static  final String EXCHANGE_NAME = "direct_logs";

 public static void main(String[] args) throws  Exception{
     Channel channel = RabbitMqUtils.getChannel();
     channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);

     Scanner scanner = new Scanner(System.in);
     while (scanner.hasNext()){
         String message = scanner.next();
         channel.basicPublish(EXCHANGE_NAME,"info",null,message.getBytes(StandardCharsets.UTF_8));
         System.out.println("生产者发出的消息："+ message);
     }
 }
}

```
> 消费者

```
public class ReceiveLogsDirect01 {
 public static final String EXCHANGE_NAME = "direct_logs";

 public static void main(String[] args) throws Exception {
     Channel channel = RabbitMqUtils.getChannel();

     //声明一个队列
     channel.queueDeclare("console",false,false,false,null);

     //绑定交换机与队列
     channel.queueBind("console",EXCHANGE_NAME,"info");
     channel.queueBind("console",EXCHANGE_NAME,"warning");


     DeliverCallback deliverCallback = (consumerTag, message) -> {
         System.out.println("ReceiveLogsDirect01控制台打印接受到的消息：" + new String(message.getBody()));
     };

     channel.basicConsume("console",true,deliverCallback,consumerTag -> {});
 }
}

```
```
public class ReceiveLogsDirect02 {
 public static final String EXCHANGE_NAME = "direct_logs";

 public static void main(String[] args) throws Exception {
     Channel channel = RabbitMqUtils.getChannel();

     //声明一个队列
     channel.queueDeclare("disk",false,false,false,null);

     //绑定交换机与队列
     channel.queueBind("disk",EXCHANGE_NAME,"error");


     DeliverCallback deliverCallback = (consumerTag, message) -> {
         System.out.println("ReceiveLogsDirect02控制台打印接受到的消息：" + new String(message.getBody()));
     };

     channel.basicConsume("disk",true,deliverCallback,consumerTag -> {});
 }
}

```
## **5.6. Topics **
### **5.6.1. 之前类型的问题**
在上一个小节中，我们改进了日志记录系统。我们没有使用只能进行随意广播的 fanout 交换机，而是 
使用了 direct 交换机，从而有能实现有选择性地接收日志。
尽管使用direct 交换机改进了我们的系统，但是它仍然存在局限性-比方说我们想接收的日志类型有 
info.base 和 info.advantage，某个队列只想 info.base 的消息，那这个时候direct 就办不到了。这个时候 
就只能使用 topic 类型 
### **5.6.2. Topic 的要求**
发送到类型是 topic 交换机的消息的 routing_key 不能随意写，必须满足一定的要求，它**必须是一个单 **
**词列表，以点号分隔开**。这些单词可以是任意单词，比如说："stock.usd.nyse", "nyse.vmw", 
"quick.orange.rabbit".这种类型的。当然这个单词列表最多不能超过 255 个字节。 
在这个规则列表中，其中有两个替换符是大家需要注意的 
***(星号)可以代替一个单词 **
**#(井号)可以替代零个或多个单词 **
### **5.6.3. Topic 匹配案例**
下图绑定关系如下 
Q1-->绑定的是 
中间带 orange 带 3 个单词的字符串(*.orange.*) 
Q2-->绑定的是 
最后一个单词是 rabbit 的 3 个单词(*.*.rabbit) 
第一个单词是 lazy 的多个单词(lazy.#)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670321363991-ed2dba5c-baf7-4816-b470-aa1aec186c8b.png#averageHue=%23f4e1e0&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=185&id=u69cc10c7&margin=%5Bobject%20Object%5D&name=image.png&originHeight=369&originWidth=1087&originalType=binary&ratio=1&rotation=0&showTitle=false&size=154834&status=done&style=none&taskId=u8d8273b0-a34a-431d-9116-a2dc8baab52&title=&width=543.5)
上图是一个队列绑定关系图，我们来看看他们之间数据接收情况是怎么样的 
> quick.orange.rabbit       被队列 Q1Q2 接收到 
> lazy.orange.elephant      被队列 Q1Q2 接收到
> quick.orange.fox      被队列 Q1 接收到 
> lazy.brown.fox      被队列 Q2 接收到 
> lazy.pink.rabbit      虽然满足两个绑定但只被队列 Q2 接收一次 
> quick.brown.fox      不匹配任何绑定不会被任何队列接收到会被丢弃 
> quick.orange.male.rabbit      是四个单词不匹配任何绑定会被丢弃 
> lazy.orange.male.rabbit      是四个单词但匹配 Q2

当队列绑定关系是下列这种情况时需要引起注意 
**当一个队列绑定键是#,那么这个队列将接收所有数据，就有点像 fanout 了 **
**如果队列绑定键当中没有#和*出现，那么该队列绑定类型就是 direct 了 **
### **5.6.4. 实战**
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670321430777-bf4feed0-bb6a-4457-af60-950e8a7cb746.png#averageHue=%23f1f0ef&clientId=udfd98a1e-3694-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=248&id=u9172c305&margin=%5Bobject%20Object%5D&name=image.png&originHeight=496&originWidth=541&originalType=binary&ratio=1&rotation=0&showTitle=false&size=55503&status=done&style=none&taskId=u88eb14d0-aa55-418d-b2c2-ab667fbef81&title=&width=270.5)
> 生产者

```
public class EmitLogTopic {
 //交换机的名称
 public static final String EXCHANGE_NAME = "topic_logs";

 public static void main(String[] args)  throws Exception{
     Channel channel = RabbitMqUtils.getChannel();


     HashMap<String, String> map = new HashMap<>();
     map.put("quick.orange.rabbit","被队列Q1Q2接收到");
     map.put("quick.orange.fox","被队列Q1接收到");
     map.put("lazy.brown.fox","被队列Q2接收到 ");
     map.put("lazy.pink.rabbit","虽然满足队列Q2的两个绑定但是只会被接收一次");
     map.put("quick.orange.male.rabbit","四个单词不匹配任何绑定会被丢弃");

     for (Map.Entry<String, String> bindingKeyEntry : map.entrySet()) {
         String routingKey = bindingKeyEntry.getKey();
         String message = bindingKeyEntry.getValue();

         channel.basicPublish(EXCHANGE_NAME,routingKey,null,message.getBytes(StandardCharsets.UTF_8));
         System.out.println("生产者发送消息："+ message );

     }
  }
}

```
> 消费者

```
/*
* 声明主题交换机及相关队列
* 消费者C1
* */
public class ReceiveLogsTopic01 {
    //交换机名称
    public static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args)  throws Exception{
        Channel channel = RabbitMqUtils.getChannel();

        //声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        //声明队列
        String queueName = "Q1";
        channel.queueDeclare(queueName,false,false,false,null);

        //队列捆绑
        channel.queueBind(queueName,EXCHANGE_NAME,"*.orange.*");
        System.out.println("等待接收消息......");

        DeliverCallback deliverCallback = (consumerTag,message) -> {
            System.out.println(new String(message.getBody()));
            System.out.println("接收队列："+ queueName + "绑定键：" + message.getEnvelope().getRoutingKey());
        };

        //接收消息
        channel.basicConsume(queueName,true,deliverCallback,consumerTag -> {});
    }
}

```
```
/*
* 声明主题交换机及相关队列
* 消费者C2
* */
public class ReceiveLogsTopic02 {
    //交换机名称
    public static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args)  throws Exception{
        Channel channel = RabbitMqUtils.getChannel();

        //声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        //声明队列
        String queueName = "Q2";
        channel.queueDeclare(queueName,false,false,false,null);

        //队列捆绑
        channel.queueBind(queueName,EXCHANGE_NAME,"*.*.rabbit");
        channel.queueBind(queueName,EXCHANGE_NAME,"*lazy.#");
        System.out.println("等待接收消息......");

        DeliverCallback deliverCallback = (consumerTag,message) -> {
            System.out.println(new String(message.getBody()));
            System.out.println("接收队列："+ queueName + "绑定键：" + message.getEnvelope().getRoutingKey());
        };

        //接收消息
        channel.basicConsume(queueName,true,deliverCallback,consumerTag -> {});
    }
}

```

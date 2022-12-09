> 将用 Java 编写两个程序。发送单个消息的生产者和接收消息并打印 
> 出来的消费者,介绍 Java API 中的一些细节。

在下图中，“ P”是我们的生产者，“ C”是我们的消费者。中间的框是一个队列-RabbitMQ 代 
表使用者保留的消息缓冲区
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670139854714-35fabd8b-7cca-498b-817b-7ecbaf433699.png#averageHue=%23e9e9e6&clientId=ud4cf5215-5f47-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=84&id=u7d742d32&margin=%5Bobject%20Object%5D&name=image.png&originHeight=168&originWidth=1128&originalType=binary&ratio=1&rotation=0&showTitle=false&size=83205&status=done&style=none&taskId=uc0bf7693-f948-4d3a-990b-56d420d7997&title=&width=564)
# 创建一个maven项目
## **2.1. 依赖**
```java
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>8</source>
          <target>8</target>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <dependencies>
    <dependency>
      <groupId>com.rabbitmq</groupId>
      <artifactId>amqp-client</artifactId>
      <version>5.8.0</version>
    </dependency>

    <dependency>
      <groupId>commons-io</groupId>
      <artifactId>commons-io</artifactId>
      <version>2.6</version>
    </dependency>
  </dependencies>
```
## **2.2. 消息生产者**
```java
public class Producer {
    // 队列名称
    public static  final String QUEUE_NAME="hello";

    // 发消息
    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();

        // 工厂IP连接RabbitMQ的队列
        factory.setHost("服务器ip");
        // 用户名
        factory.setUsername("admin");
        // 密码
        factory.setPassword("123456");

        factory.setPort(5672);

        // 创建连接
        Connection connection = factory.newConnection();
        // 获取信道
        Channel channel = connection.createChannel();
        /*
         * 生成一个队列
         * 参数1：队列名称
         * 参数2：队列里面的消息是否持久化，默认情况下，消息存储在内存中
         * 参数3：该队列是否只供一个消费者进行消费，是否进行消费共享，true可以多个消费者消费，
         *        false只能一个消费者消费
         * 参数4：是否自动删除：最后一个消费者断开连接之后，该队列是否自动删除，true则自动删除，
         *        false不自动删除
         * 参数5：其他参数
         * */
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);
        // 发消息
        String message = "hello world";
        /*
         * 发送一个消息
         * 参数1：发送到哪个交换机
         * 参数2：路由的key值是那个，本次是队列的名称
         * 参数3：其他参数信息
         * 参数4：发送消息的消息体
         * */
        channel.basicPublish("",QUEUE_NAME,null,message.getBytes(StandardCharsets.UTF_8));
        System.out.println("消息发送完毕！");

    }
}


```
#### 运行结果:
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670140044698-c4b3a09e-d66b-4b2b-9503-ad448097c3e5.png#averageHue=%23324330&clientId=ud4cf5215-5f47-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=142&id=u707bdce5&margin=%5Bobject%20Object%5D&name=image.png&originHeight=283&originWidth=1571&originalType=binary&ratio=1&rotation=0&showTitle=false&size=444535&status=done&style=none&taskId=u196003d8-33bf-4658-a562-b0f79191293&title=&width=785.5)
## **2.3. 消息消费者**
```
/**
 * @author xingchen
 * @version V1.0
 * @Package com.xingchen.mq
 * @date 2022/12/4 15:37
 */
public class Consumer {
    // 队列名称
    public static final String QUEUE_NAME = "hello";

    // 接受消息
    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("110.40.211.224");
        factory.setUsername("admin");
        factory.setPassword("123456");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 声明 接受消息
        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println(new String(message.getBody()));
        };

        // 声明 取消消息
        CancelCallback cancelCallback = consumer -> {
            System.out.println("消息消费被中断");
        };

        /*
         * 消费者接收消息
         * 参数1：表示消费哪个UI列
         * 参数2：消费成功之后，是否需要自动应答，true表示自动应答，false表示手动应答
         * 参数3：消费者成功消费的回调
         * 参数4：消费者取消消费的回调
         */
        channel.basicConsume(QUEUE_NAME,true,deliverCallback,cancelCallback);
    }
}
```
#### 运行结果
![image.png](https://cdn.nlark.com/yuque/0/2022/png/33764834/1670140056029-726bb2ef-6b65-47ff-89c9-20c42dbc6ba2.png#averageHue=%23535d4e&clientId=ud4cf5215-5f47-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=162&id=u0523da99&margin=%5Bobject%20Object%5D&name=image.png&originHeight=323&originWidth=1574&originalType=binary&ratio=1&rotation=0&showTitle=false&size=482762&status=done&style=none&taskId=u532e02d1-103a-4da1-9b87-b7c4ae135a4&title=&width=787)

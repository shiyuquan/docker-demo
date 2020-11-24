package persion.bleg.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * spring.rabbitmq.host: 服务Host
 * spring.rabbitmq.port: 服务端口
 * spring.rabbitmq.username: 登陆用户名
 * spring.rabbitmq.password: 登陆密码
 * spring.rabbitmq.virtual-host: 连接到rabbitMQ的vhost
 * spring.rabbitmq.addresses: 指定client连接到的server的地址，多个以逗号分隔(优先取addresses，然后再取host)
 * spring.rabbitmq.requested-heartbeat: 指定心跳超时，单位秒，0为不指定；默认60s
 * spring.rabbitmq.publisher-confirms: 是否启用【发布确认】
 * spring.rabbitmq.publisher-returns: 是否启用【发布返回】
 * spring.rabbitmq.connection-timeout: 连接超时，单位毫秒，0表示无穷大，不超时
 * spring.rabbitmq.parsed-addresses:
 * <p>
 * <p>
 * # ssl
 * spring.rabbitmq.ssl.enabled: 是否支持ssl
 * spring.rabbitmq.ssl.key-store: 指定持有SSL certificate的key store的路径
 * spring.rabbitmq.ssl.key-store-password: 指定访问key store的密码
 * spring.rabbitmq.ssl.trust-store: 指定持有SSL certificates的Trust store
 * spring.rabbitmq.ssl.trust-store-password: 指定访问trust store的密码
 * spring.rabbitmq.ssl.algorithm: ssl使用的算法，例如，TLSv1.1
 * <p>
 * <p>
 * # cache
 * spring.rabbitmq.cache.channel.size: 缓存中保持的channel数量
 * spring.rabbitmq.cache.channel.checkout-timeout: 当缓存数量被设置时，从缓存中获取一个channel的超时时间，单位毫秒；如果为0，则总是创建一个新channel
 * spring.rabbitmq.cache.connection.size: 缓存的连接数，只有是CONNECTION模式时生效
 * spring.rabbitmq.cache.connection.mode: 连接工厂缓存模式：CHANNEL 和 CONNECTION
 * <p>
 * <p>
 * # listener
 * spring.rabbitmq.listener.simple.auto-startup: 是否启动时自动启动容器
 * spring.rabbitmq.listener.simple.acknowledge-mode: 表示消息确认方式，其有三种配置方式，分别是none、manual和auto；默认auto
 * spring.rabbitmq.listener.simple.concurrency: 最小的消费者数量
 * spring.rabbitmq.listener.simple.max-concurrency: 最大的消费者数量
 * spring.rabbitmq.listener.simple.prefetch: 指定一个请求能处理多少个消息，如果有事务的话，必须大于等于transaction数量.
 * spring.rabbitmq.listener.simple.transaction-size: 指定一个事务处理的消息数量，最好是小于等于prefetch的数量.
 * spring.rabbitmq.listener.simple.default-requeue-rejected: 决定被拒绝的消息是否重新入队；默认是true（与参数acknowledge-mode有关系）
 * spring.rabbitmq.listener.simple.idle-event-interval: 多少长时间发布空闲容器时间，单位毫秒
 * <p>
 * spring.rabbitmq.listener.simple.retry.enabled: 监听重试是否可用
 * spring.rabbitmq.listener.simple.retry.max-attempts: 最大重试次数
 * spring.rabbitmq.listener.simple.retry.initial-interval: 第一次和第二次尝试发布或传递消息之间的间隔
 * spring.rabbitmq.listener.simple.retry.multiplier: 应用于上一重试间隔的乘数
 * spring.rabbitmq.listener.simple.retry.max-interval: 最大重试时间间隔
 * spring.rabbitmq.listener.simple.retry.stateless: 重试是有状态or无状态
 * <p>
 * <p>
 * # template
 * spring.rabbitmq.template.mandatory: 启用强制信息；默认false
 * spring.rabbitmq.template.receive-timeout: receive() 操作的超时时间
 * spring.rabbitmq.template.reply-timeout: sendAndReceive() 操作的超时时间
 * spring.rabbitmq.template.retry.enabled: 发送重试是否可用
 * spring.rabbitmq.template.retry.max-attempts: 最大重试次数
 * spring.rabbitmq.template.retry.initial-interval: 第一次和第二次尝试发布或传递消息之间的间隔
 * spring.rabbitmq.template.retry.multiplier: 应用于上一重试间隔的乘数
 * spring.rabbitmq.template.retry.max-interval: 最大重试时间间隔
 *
 * @author shiyuquan
 * @since 2020/9/2 11:33 上午
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitMqConfig {

    /** 交换机名称 */
    public static final String TEST_TOPIC_EXCHANGE = "test_topic";

    /** 死信交换机 */
    public static final String DEAD_LETTER_EXCHANGE = "dead_letter_exchange";

    /** 队列名称 */
    public static final String TEST_QUEUE = "test_queue";

    /**
     *  死信队列
     *
     *  rabbitmq没有直接提供延迟队列的功能
     *  一个可以做到延迟队列的方式就是利用死信队列
     *  针对Queue设置x-expires 或者 针对Message设置 x-message-ttl，来控制消息的生存时间，如果超时(两者
     *  同时设置以最先到期的时间为准)，则消息变为dead letter(死信)，之后处理死信队列就可以了
     *
     */
    public static final String DEAD_LETTER_QUEUE = "dead_letter_queue";

    /**
     * 声明交换机
     */
    @Bean
    public Exchange testTopicExchange() {
        return ExchangeBuilder.topicExchange(TEST_TOPIC_EXCHANGE).durable(true).build();
    }

    /**
     * 声明死信交换机
     * 死信队列跟交换机类型没有关系 不一定为directExchange  不影响该类型交换机的特性.
     */
    @Bean
    public Exchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    /** 死信息 队列 */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    /**
     * 绑定死信息队列和死信息交换机
     */
    @Bean
    public Binding bindDeadLetterQuweue2DeadLetterTopicExchange() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("test.dead.message.*").noargs();
    }

    /**
     * 声明队列
     */
    @Bean
    public Queue testQueue() {
        return QueueBuilder.durable(TEST_QUEUE)
                // 绑定死信交换机
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                // 指定死信队列的路由
                .withArgument("x-dead-letter-routing-key", "test.dead.message.*")
                .build();
    }

    /**
     * 绑定队列和交换机
     */
    @Bean
    public Binding bindTestQueue2TestTopicExchange() {
        return BindingBuilder.bind(testQueue()).to(testTopicExchange()).with("test.*").noargs();
    }



    private ConnectionFactory connectionFactory(RabbitProperties rabbitProperties) {
        return createConnectionFactory(rabbitProperties.getHost(), rabbitProperties.getPort(),
                rabbitProperties.getUsername(), rabbitProperties.getPassword(), rabbitProperties.getVirtualHost());
    }

    private ConnectionFactory createConnectionFactory(String hose, int port, String username, String pwd,
                                                      String vHost) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(hose);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(pwd);
        connectionFactory.setVirtualHost(vHost);
        return connectionFactory;
    }

    /**
     * setCacheMode: 设置缓存模式，共有两种，CHANNEL和CONNECTION模式。
     * <p>
     * CHANNEL模式，程序运行期间ConnectionFactory会维护着一个Connection,所有的操作都会使用这个Connection，但一个Connection
     * 中可以有多个Channel，操作rabbitmq之前都必须先获取到一个Channel，否则就会阻塞（可以通过setChannelCheckoutTimeout ()
     * 设置等待时间），这些Channel会被缓存（缓存的数量可以通过setChannelCacheSizeO设置）；
     * <p>
     * CONNECTION模式，这个模式下允许创建多个Connection，会缓存一定数量的Connection，每个Connection中同样会缓存一些Channel，
     * 除了可以有多个Connection，其它都跟CHANNEL模式一样。
     * <p>
     * setChannelCacheSize:设置每个Connection中（注意是每个Connection)可以缓存的Channel数量，注意只是缓存的Channel数量，
     * 不是Channel的数量上限，操作rabbitmq之前（send/receive message等）要先获取到一个Channel，获取Channel时会先从缓存中
     * 找闲置的Channel，如果没有则创建新的Channel，当Channel数量大于缓存数量时，多出来没法放进缓存的会被关闭。注意，改变
     * 这个值不会影响己经存在的Connection，只影响之后创建的Connection。
     * <p>
     * setChannelCheckoutTimeout:当这个值大于0时，charmelCacheSize不仅是缓存数量，同时也会变成数量上限，从缓存获取不到y
     * 用的Channel时，不会创建新的Channel，会等待这个值设置的毫秒数，到时间仍然获取不到可用的Channel会抛出AmQpTimeoutException
     * 异常。同时，在CONNECTION模式，这个值也会影响获取Connect ion的等待时间，超时获取不到Connect ion也会抛出AmqpTimeoutExcept ion
     * 异常。
     * <p>
     * setPublisherReturns、setPublisherConfirms: producer端的消息确认机制（ confirm和return)，设为true后开启相应的机制。
     * 官方文档描述publisherReturns设为true打开return机制，publisherComfirms设为true打开confirm机制,但测试结果（2.0.5.RELEASE版本)
     * 是任意一个设为true，两个都会打开。
     * <p>
     * addCormectionListener、siddCharmelListener、setRecoveryListener:添加或设置相应的Listener，后文详述。
     * <p>
     * setConnectionCacheSize：仅在CONNECTION模式使用，设置Connection的缓存数量。
     * <p>
     * setConnectionLimit: 仅在CONNECTION模式使用，设置Connection的数量上限。
     */
    @Bean
    public CachingConnectionFactory cachingConnectionFactory(RabbitProperties rabbitProperties) {
        ConnectionFactory connectionFactory = connectionFactory(rabbitProperties);
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
        cachingConnectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
        cachingConnectionFactory.setChannelCacheSize(25);
        cachingConnectionFactory.setChannelCheckoutTimeout(0);
        cachingConnectionFactory.setPublisherReturns(true);
        cachingConnectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        cachingConnectionFactory.setConnectionCacheSize(1);
        cachingConnectionFactory.setConnectionLimit(1000);
        return cachingConnectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin(CachingConnectionFactory cachingConnectionFactory) {
        return new RabbitAdmin(cachingConnectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(MessageConverter messageConverter,
                                         CachingConnectionFactory cachingConnectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setConnectionFactory(cachingConnectionFactory);
        // 设置重连机制 setRetryTemplate(retryTemplate);
        // 设置messageConverter用于对象与message对象之间的相互转换
        template.setMessageConverter(messageConverter);
        // 打开channel的事务
        template.setChannelTransacted(false);
        // mandatory参数，默认为false。设置为true时，如果消息无法路由到相应队列，rabbitmci会返回一个return给发送方，发
        // 送方可以通过设置ReturnListener 获取 ReturnCallback 处理return。
        // 如果为false，消息路由不到队列，则消息将被丢弃。
        template.setMandatory(true);
        // 当服务同时作为发送端和接收端，建议作为双端时使用不同的connect ion
        template.setUsePublisherConnection(true);
        return template;
    }

    /**
     * 这个bean仅在consumer端通过@RabbitListener注解的方式接收消息时使用，每一个@RabbitListener注解的方法都会由这个
     * RabbitListenerContainerFactory创建一个MessageListenerContainer，负责接收消息。
     * <p>
     * setConnectionFactory：设置spring-amqp的ConnectionFactory。
     * <p>
     * setMessageConverter: 对于consumer端，MessageConverter也可以在这里配置。
     * <p>
     * setAcknowledgeMode: 设置consumer端的应答模式，共有三种：NONE、AUTO、MANUAL。
     * NONE: 无应答，这种模式下rabbitmq默认consumer能正确处理所有发出的消息，所以不管消息有没有被consumer收到，有没有正确处理都不会恢复;
     * AUTO: 由Container自动应答，正确处理发出ack信息，处理失败发出nack信息，rabbitmq发出消息后将会等待consumer端的应答，
     * 只有收到ack确认信息才会把消息清除掉，收到nack信息的处理办法由setDefaultRequeueRejected()方法设置，所以在这种模式下，
     * 发生错误的消息是可以恢复的。
     * MANUAL: 基本同AUTO模式，区别是需要人为调用方法给应答。
     * <p>
     * setConcurrentConsumers: 设置每个MessageListenerContainer将会创建的Consumer的最小数量，默认是1 个。
     * setMaxConcurrentConsumers: 设置每个MessageListenerContainer将会创建的Consumer的最大数量，默认等于最小数量。
     * <p>
     * setPrefetchCount: 设置信道上的消费者所能保持的最大未确认消息数量
     * <p>
     * setChannelTransacted: 设置Channel的事务。
     * <p>
     * setTxSize: 设置事务当中可以处理的消息数量。
     * setBatchSize
     * <p>
     * setDefaultRequeueRejected: 设置当rabbitmq收到nack/reject确认信息时的处理方式，设为true，扔回Queue头部，设为false，丢弃 。
     * <p>
     * setErrorHandler: 实现ErrorHandler接口设置进去，所有未catch的异常都会由ErrorHandler处理。
     */
    @Bean
    @Autowired
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(CachingConnectionFactory cachingConnectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        //设置连接工厂
        factory.setConnectionFactory(cachingConnectionFactory);
        //设置消息转换
        factory.setMessageConverter(messageConverter);
        //设置消费端应答模式none，auto, manual
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(250);
        factory.setChannelTransacted(false);
        factory.setBatchSize(1);
        factory.setDefaultRequeueRejected(true);
        return factory;
    }

    /**
     * 推荐用JsonMessageConverter、Jackson2JsonMessageConverter，这两个是都将java对象转化为json再转为byte[]来构造Message
     * 对象，前一个用的是jacksonjsonlib，后一个用的是jackson2jsonlib()
     */
    @Bean
    public MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    /**
     * 有两个error handler类可以对@RabbitListener注解的方法中拋出的异常进行处理。
     * <p>
     * 一个是 RabbitListenerErrorHandler接口，并将其设置到@RabbitListener注解中
     * <p>
     * @RabbitListener注解的方法中抛出的异常，首生会讲入RabbitListenerErrorHandler,这里如里没有能力处理这个异常,需要将
     * 其重新抛出（否则不会进入ErrorHandler)，然后异常将会进入ErrorHandler，一旦异常进入ErrorHandler就意味着消息消费失畋
     * 了（所以不需要重新抛出异常）。
     * RabbitListenerErrorHandler没有默认配置，ErrorHandler有一个默认的ConditionalRejectingErrorHandler类，它的处理方式是
     * 打印曰志，然后辨别异常类型，如果属于以下几种异常
     * <p>
     * o. s. amqp... MessageConversionException
     * o. s. messaging... MessageConversionException
     * o. s. messaging... MethodArgumentNotValidException
     * o. s. messaging... MethodArgumentTypeMismatchException
     * java. lang. NoSuchMethodException
     *
     * 则包装成AmqpRejectAndDontRequeueException拋出
     * 这个异常的作用是，忽略defaultRequeueRejected的设置，强制让rabbitmq丢弃此条处理失败消息，不放回queue。
     * <p>
     * 这样处理是因为这些异常是不可挽回的，就算再重新执行也一样会拋异常，如果放回到Queue就会陷入“消费失败-放回queue-消费
     * 失畋... ”的死循环
     */
    @Bean
    public RabbitListenerErrorHandler rabbitListenerErrorHandler() {
        return (amqpMessage, message, exception) -> {
            log.error("RabbitListenerErrorHandler -> data: {}, message: {}, exception: {}", amqpMessage, message, exception);
            // 处理@RabbitListener标记的方法拋出的异常
            return null;
        };
    }

    /**
     * ConfirmCallback -每~条发出的消息都会调用ConfirmCallback;
     * ReturnCallback -只有在消息进入exchange但没有进入queue时才会调用。
     * 相关方法入参•.
     * correlationData - RabbitTemplate的send系列方法中有带这个参数的，如果传了这个参数，会在回调时拿到;
     * ack -消息进入exchange,为true,未能进入exchange，为false，由于Connection中断发出的消息进入exchange但没有收至(J
     * confirm信息的情况，也会是false;
     * cause -消息发送失败时的失败原因信息。
     * <p>
     * 异步的接收confirm和return时仍然需要走原来发送消息用到的那个Channel，如果那个Channel被关闭了，是收不到confirm/return
     * 信息的。好在根据以上说明，Channel会等到最后一个confirm接收到时才会close，所以应该也不用担心Channel被关闭而接收不
     * 到con firm的问題
     */
    public RabbitTemplate.ReturnCallback returnCallback() {
        return (message, replyCode, replyText, exchange, routingKey) -> {
            //通过了交换器，而未能到达queue的消息，认为是游离消息，进行游离消息持久化M也
            log.error("return message: {}，reply code： {}，reply text： {}，exchange ： {}，routing key： {}",
                    new String(message.getBody()), replyCode, replyText, exchange, routingKey);
        };
    }

    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(Integer.MAX_VALUE);
        retryTemplate.setRetryPolicy(simpleRetryPolicy);
        return retryTemplate;
    }

    /**
     * Listener
     *
     * ChannelListener接口，监听Channel的创建和异常关闭。
     *
     * BlockedListener监听Connect ion 的block和unblock。
     *
     * ConnectionListener监听Connection的创建、关闭和异常终止。
     *
     * RecoveryListener监听开始自动恢复Connect ion、自动恢复连接完成。
     *
     * ConnectionListener、ChannelListener、Recoverytistener设置到ConnectionFactory即可。
     */

}


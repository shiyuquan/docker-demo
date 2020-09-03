package persion.bleg.dockerdemo.rabbitmq.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author shiyuquan
 * @since 2020/9/2 10:27 下午
 */
@Component
@Slf4j
public class RabbitMqService {
    private MyConfirmCallback confirmCallback;
    private MyReturnCallback returnCallback;
    private RabbitTemplate rabbitTemplate;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setReturnCallback(returnCallback);
        this.rabbitTemplate.setConfirmCallback(confirmCallback);
        // this.rabbitTemplate.setRetryTemplate();
    }

    @Autowired
    public void setConfirmCallback(MyConfirmCallback confirmCallback) { this.confirmCallback = confirmCallback; }
    @Autowired
    public void setReturnCallback(MyReturnCallback returnCallback) { this.returnCallback = returnCallback; }

    /**
     * @param exchange 交换机
     * @param routingKey 路由
     * @param body 消息
     */
    public void send(String exchange, String routingKey, Object body) {
        MessageProperties properties = new MessageProperties();
        // 设置消息的id,该id将在消费者端用于去重！
        properties.setMessageId(UUID.randomUUID().toString());
        Message message = rabbitTemplate.getMessageConverter().toMessage(body, properties);
        PublicCorrelationData correlationData = new PublicCorrelationData(exchange, routingKey, message);
        correlationData.setId(UUID.randomUUID().toString());
        send(correlationData);
    }

    public boolean send(PublicCorrelationData correlationData) {
        try {
            rabbitTemplate.send(correlationData.getExchange(),
                    correlationData.getRoutingKey(),
                    correlationData.getMessage(),
                    correlationData);
            log.info("message send success: {}", correlationData);
            return true;
        } catch (AmqpConnectException e) {
            // 持久化消息
            log.error("message send faild: {}", correlationData);
            return false;
        }
    }
}

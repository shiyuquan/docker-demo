package persion.bleg.rabbitmq.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息发送到交换器时触发
 *
 * @author shiyuquan
 * @since 2020/9/2 10:59 下午
 */
@Slf4j
@Component
public class MyConfirmCallback implements RabbitTemplate.ConfirmCallback {

    /**
     * Confirmation callback.
     *
     * @param correlationData correlation data for the callback.
     * @param ack             true for ack, false for nack
     * @param cause           An optional cause, for nack, when available, otherwise null.
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData instanceof PublicCorrelationData) {
            PublicCorrelationData correlationDatal = (PublicCorrelationData) correlationData;
            String exchange = correlationDatal.getExchange();
            String routingKey = correlationDatal.getRoutingKey();
            Message message = correlationDatal.getMessage();
            if (!ack) {
                // ack为false的时候，代表消息发送失败，需要重发
                log.error("message send error： messageld： {}, exchange ： {}，routingKey： {} cause： {}", message.getMessageProperties().getMessageId(), exchange, routingKey, cause);
                // 持久化消息 todo
            }
        }
    }
}

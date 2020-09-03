package persion.bleg.dockerdemo.rabbitmq.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息从交换器发送到队列失败时触发
 *
 * @author shiyuquan
 * @since 2020/9/2 10:59 下午
 */
@Slf4j
@Component
public class MyReturnCallback implements RabbitTemplate.ReturnCallback {

    /**
     * Returned message callback.
     *
     * @param message    the returned message.
     * @param replyCode  the reply code.
     * @param replyText  the reply text.
     * @param exchange   the exchange.
     * @param routingKey the routing key.
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 通过了交换器，而未能到达queue的消息，认为是游离消息，进行游离消息持久化 todo
        log.error("return message: {}，reply code： {}，reply text： {}，exchange ： {}, routing key： {}",
                new String(message.getBody()), replyCode, replyText, exchange, routingKey);
    }
}

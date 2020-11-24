package persion.bleg.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import persion.bleg.boot.base.BlegException;
import persion.bleg.rabbitmq.RabbitMqConfig;

import java.io.IOException;

/**
 *
 * channel.basicReject(long, requeue);
 *
 * @author shiyuquan
 * @since 2020/9/2 1:45 下午
 */
@Component
@Slf4j
public class RabbitConsumer {

    /**
     * 注意：这个方法内拋出异常基本上消息会回到队列，之后依旧会出现问题，所以对方法内的每一步操作应该都有明确的标准
     * 手动ack或者打印曰志
     */
    @RabbitHandler
    @RabbitListener(queues = RabbitMqConfig.TEST_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void getQmsBaseMessage(@Payload String body, Message message, Channel channel) {
        try {
            log.info("get message: " + body);

            // 实现业务
            if ("err".equals(body)) {
                throw new BlegException("消费失败");
            }

            /**
             * 无异常就确认消息
             * basicAck(long deliveryTag, boolean multiple)
             * deliveryTag: 取出来当前消息在队列中的的索引;
             *
             * multiple: 为true的话就是批量确认,例如如果当前deliveryTag为5,那么就会确认
             *          deliveryTag为5及其以下的消息;一般设置为false
             */
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

            log.info("ack message {}", message);
        } catch (Exception e) {

            /**
             * 有异常就绝收消息
             *
             * basicNack(long deliveryTag, boolean multiple, boolean requeue)
             * deliveryTag: 取出来当前消息在队列中的的索引;
             *
             * multiple: 为true的话就是批量确认,例如如果当前deliveryTag为5,那么就会确认
             *           deliveryTag为5及其以下的消息;一般设置为false
             * requeue: true为将消息重返当前消息队列,还可以重新发送给消费者;
             *         false:将消息丢弃
             */
            try {
                // requeue 设置为fasle，绑定死信队列，消费失败的消息会发到死信息队列做后置处理。
                // 这里设置重试机制其实没什么用了，一般代码一次执行不成功，那么大部分情况下，后面的一样不成功
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                // throw new BlegException("rabbit消息 nack 失败, message: " + message);
            } catch (IOException ex) {
                ex.printStackTrace();
                // throw new BlegException("rabbit消息 nack 失败, message: " + message);
            }
        }
    }

    /**
     * 死信队列的处理
     */
    @RabbitHandler
    @RabbitListener(queues = RabbitMqConfig.DEAD_LETTER_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleDeadLetter(@Payload String body, Message message, Channel channel) throws IOException {
        log.error("处理死信: body: {}", body);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}

package persion.bleg.rabbitmq.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * @author shiyuquan
 * @since 2020/9/2 10:43 下午
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PublicCorrelationData extends CorrelationData {
    private String exchange;
    private String routingKey;
    private Message message;
}

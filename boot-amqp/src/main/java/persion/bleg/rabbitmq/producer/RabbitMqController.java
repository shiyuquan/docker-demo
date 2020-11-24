package persion.bleg.rabbitmq.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import persion.bleg.rabbitmq.RabbitMqConfig;

/**
 * @author shiyuquan
 * @since 2020/9/2 11:47 上午
 */
@Component
public class RabbitMqController {
    RabbitMqService rabbitMqService;

    @Autowired
    public void setRabbitMqService(RabbitMqService rabbitMqService) {
        this.rabbitMqService = rabbitMqService;
    }

    /**
     * 测试
     */
    public String sendMsgTest(String msg, String key){
        rabbitMqService.send(RabbitMqConfig.TEST_TOPIC_EXCHANGE ,key ,msg);
        return "发送消息成功！";
    }
}

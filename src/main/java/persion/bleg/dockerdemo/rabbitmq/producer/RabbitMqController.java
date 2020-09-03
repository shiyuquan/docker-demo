package persion.bleg.dockerdemo.rabbitmq.producer;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import persion.bleg.dockerdemo.rabbitmq.RabbitMqConfig;

import static persion.bleg.dockerdemo.constants.DefalutConstant.DEFAULT_API_PREFIX;

/**
 * @author shiyuquan
 * @since 2020/9/2 11:47 上午
 */
@Api(tags = "rabbitmq 测试接口")
@RestController
@RequestMapping(DEFAULT_API_PREFIX + "/rabbitmq")
public class RabbitMqController {
    RabbitMqService rabbitMqService;

    @Autowired
    public void setRabbitMqService(RabbitMqService rabbitMqService) {
        this.rabbitMqService = rabbitMqService;
    }

    /**
     * 测试
     */
    @GetMapping("/sendmsg")
    public String sendMsg(@RequestParam String msg, @RequestParam String key){
        rabbitMqService.send(RabbitMqConfig.TEST_TOPIC_EXCHANGE ,key ,msg);
        return "发送消息成功！";
    }
}

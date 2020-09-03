package persion.bleg.dockerdemo.mqtt;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static persion.bleg.dockerdemo.constants.DefalutConstant.DEFAULT_API_PREFIX;

/**
 * @author shiyuquan
 * @since 2020/9/1 11:27 上午
 */
@Slf4j
@Api(tags = "mqtt 测试接口")
@RestController
@RequestMapping(DEFAULT_API_PREFIX + "/mqtt")
public class MqttController {

    private MqttSender mqttSender;

    @Autowired
    public void setMqttSender(MqttSender mqttSender) {
        this.mqttSender = mqttSender;
    }

    @PostMapping(value = "/send")
    public void send(@RequestParam String data){
        mqttSender.sendToMqtt(data);
        log.info("mqtt success");
    }
}

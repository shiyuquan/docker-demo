package persion.bleg.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author shiyuquan
 * @since 2020/9/1 11:27 上午
 */
@Slf4j
public class MqttController {

    private MqttSender mqttSender;

    @Autowired
    public void setMqttSender(MqttSender mqttSender) {
        this.mqttSender = mqttSender;
    }

    public void sendTest(String data){
        mqttSender.sendToMqtt(data);
        log.info("mqtt success");
    }
}

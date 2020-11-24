package persion.bleg.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * mqtt 配置
 *
 * QoS（Quality of Service，服务质量)
 *
 * QoS 作用于Publisher，它的值决定了server需要响应的内容。
 * QoS = 0: 至多一次，不保证消息到达，可能会丢失或重复。server没有response
 * QoS = 1: 至少一次，确保消息到达，但是可能会有重复。server向client发送PUBACK(Publish Acknowledgement)
 * QoS = 2: 只有一次，确保消息到达并只有一次。消息类型有PUBREC(Publish Received 已收到)、PUBREL(Publish Released 已释放)、
 *          PUBCOMP(Publish Completed已完成)。
 *
 * 注意：mqtt 并不能保证消息只消费一次，即便是qos=2
 *
 *
 * @author shiyuquan
 * @since 2020/9/1 10:49 上午
 */
@Slf4j
@Configuration
public class MqttConfig {

    private MqttProperty mqttProperty;

    @Autowired
    public void setMqttProperty(MqttProperty mqttProperty) {
        this.mqttProperty = mqttProperty;
    }

    @Bean
    public MqttConnectOptions getMqttConnectOptions(){
        MqttConnectOptions mqttConnectOptions=new MqttConnectOptions();

        mqttConnectOptions.setUserName(mqttProperty.getUserName());
        mqttConnectOptions.setPassword(mqttProperty.getPassword().toCharArray());
        mqttConnectOptions.setServerURIs(new String[]{mqttProperty.getUrl()});
        mqttConnectOptions.setKeepAliveInterval(mqttProperty.getKeepAliveInterval());

        // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，
        // 这里设置为true表示每次连接到服务器都以新的身份连接
        mqttConnectOptions.setCleanSession(true);

        return mqttConnectOptions;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttConnectOptions mqttConnectOptions) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(mqttConnectOptions);
        return factory;
    }

    /* 发布消息 配置 */
    @Bean
    public MessageChannel outboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "outboundChannel")
    public MessageHandler getMqttProducer(MqttPahoClientFactory mqttPahoClientFactory) {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(mqttProperty.getClientId(), mqttPahoClientFactory);
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(mqttProperty.getDefaultTopic());

        //  保留消息定义：
        // 如果 publish消息的retain标记位被设置为1，则称该消息为“保留消息”
        //
        // Broker对保留消息的处理Broker会存储每个Topic的最后一条保留消息及其Qos，当订阅该Topic的客户端上线后，Broker需要将该消息投递给它。
        // publish消息时，如果retain值是true，则服务器会一直记忆，哪怕是服务器重启。
        messageHandler.setDefaultRetained(mqttProperty.isDefaultRetained());

        messageHandler.setDefaultQos(mqttProperty.getDefaultQos());

        return messageHandler;
    }

    /* 订阅消息 配置 */
    @Bean
    public MessageChannel inboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer getMqttConsumer(MqttPahoClientFactory mqttPahoClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(mqttProperty.getClientId() + "q", mqttPahoClientFactory, "amq.topic");
        adapter.setCompletionTimeout(mqttProperty.getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(mqttProperty.getDefaultQos());
        adapter.setOutputChannel(inboundChannel());

        return adapter;
    }

    //通过通道获取数据
    @Bean
    @ServiceActivator(inputChannel = "inboundChannel")
    public MessageHandler handler() {
        return message -> log.info("主题：{}，消息接收到的数据：{}", message.getHeaders().get("mqtt_receivedTopic"), message.getPayload());
    }
}

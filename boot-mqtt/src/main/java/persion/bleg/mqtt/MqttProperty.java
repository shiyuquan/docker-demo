package persion.bleg.mqtt;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mqtt 配置类
 *
 * @author shiyuquan
 * @since 2020/9/1 10:49 上午
 */
@Configuration
@ConfigurationProperties(prefix = "mqtt")
@Data
@ToString
public class MqttProperty {

    /**
     * MQTT-用户名
     */
    private String userName;

    /**
     * MQTT-密码
     */
    private String password;

    /**
     * MQTT-服务器连接地址，如果有多个，用逗号隔开，如：tcp://127.0.0.1:61613，tcp://192.168.2.133:61613
     */
    private String url;

    /**
     * MQTT-连接服务器默认客户端ID
     */
    private String clientId;

    /**
     * MQTT-默认的消息推送主题，实际可在调用接口时指定
     */
    private String defaultTopic;

    /**
     * 连接超时
     */
    private Long completionTimeout;

    /**
     * QoS（Quality of Service，服务质量)
     *
     * QoS 作用于Publisher，它的值决定了server需要响应的内容。
     * QoS = 0: 至多一次，不保证消息到达，可能会丢失或重复。server没有response
     * QoS = 1: 至少一次，确保消息到达，但是可能会有重复。server向client发送PUBACK(Publish Acknowledgement)
     * QoS = 2: 只有一次，确保消息到达并只有一次。消息类型有PUBREC(Publish Received 已收到)、PUBREL(Publish Released 已释放)、
     *         PUBCOMP(Publish Completed已完成)。
     *
     *         注意：mqtt 并不能保证消息只消费一次，即便是qos=2
     */
    private int defaultQos = 1;

    /**
     * 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
     */
    private int keepAliveInterval;

    /**
     * 如果 publish消息的retain标记位被设置为1，则称该消息为“保留消息”
     * <p>
     * Broker对保留消息的处理Broker会存储每个Topic的最后一条保留消息及其Qos，当订阅该Topic的客户端上线后，Broker需要将该消息投递给它。
     * publish消息时，如果retain值是true，则服务器会一直记忆，哪怕是服务器重启
     */
    private boolean defaultRetained = false;
}

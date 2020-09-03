package persion.bleg.dockerdemo.mqtt;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static persion.bleg.dockerdemo.constants.DefalutConstant.MQTT;

/**
 * Mqtt 配置类
 *
 * @author shiyuquan
 * @since 2020/9/1 10:49 上午
 */
@Configuration
@ConfigurationProperties(prefix = MQTT)
@Data
@ToString
public class MqttProperty {

    /** MQTT-用户名 */
    private String userName;

    /** MQTT-密码 */
    private String password;

    /** MQTT-服务器连接地址，如果有多个，用逗号隔开，如：tcp://127.0.0.1:61613，tcp://192.168.2.133:61613 */
    private String url;

    /** MQTT-连接服务器默认客户端ID */
    private String clientId;

    /** MQTT-默认的消息推送主题，实际可在调用接口时指定 */
    private String defaultTopic;

    /** 连接超时 */
    private Long completionTimeout;

    /** 连接超时 */
    private int defaultQos;

    /** 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制 */
    private int keepAliveInterval;
}

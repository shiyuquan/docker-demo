package persion.bleg.business.maliciousrequest;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static persion.bleg.business.constants.DefalutConstant.MALICIOUS_REQUEST_CONFIG_NAME;

/**
 * @author shiyuquan
 * @since 2020/3/23 5:10 下午
 */
@Configuration
@ConfigurationProperties(prefix = MALICIOUS_REQUEST_CONFIG_NAME)
@Data
public class MaliciousRequestConfig {

    /** 是否开启功能，默认关闭 */
    private Boolean open = false;

    /** 限制时间，默认一分钟 */
    private Long limitedTime = 60 * 1000L;

    /** 时间间隔，默认一秒 */
    private Long timeInterval = 1000L;

    /** 时间间隔内允许的请求次数，默认10次 */
    private Integer safetyNum = 10;

}

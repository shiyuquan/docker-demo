package persion.bleg.dockerdemo.fdfs;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static persion.bleg.dockerdemo.constants.DefalutConstant.FAST_PROPERTY;

/**
 * @author shiyuquan
 * @since 2020/7/22 5:10 下午
 */
@Configuration
@ConfigurationProperties(prefix = FAST_PROPERTY)
@Data
@ToString
public class FdfsProperty {
    private String trackerServers;
    private String connectTimeoutInSeconds;
    private String networkTimeoutInSeconds;
    private String charset;
    private String httpAntiStealToken;
    private String httpSecretKey;
    private String httpTrackerHttpPort;
    private ConnectionPool connectionPool;
}

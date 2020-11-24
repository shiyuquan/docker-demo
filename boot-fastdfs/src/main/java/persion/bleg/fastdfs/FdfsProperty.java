package persion.bleg.fastdfs;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author shiyuquan
 * @since 2020/7/22 5:10 下午
 */
@Configuration
@ConfigurationProperties(prefix = "fastdfs")
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

package persion.bleg.dockerdemo.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static persion.bleg.dockerdemo.constants.DefalutConstant.MINIO_CONFIG_NAME;

/**
 * minio 的配置
 *
 * @author shiyuquan
 * @since 2020/4/17 2:53 下午
 */
@Data
@Configuration
@ConfigurationProperties(prefix = MINIO_CONFIG_NAME)
public class MinioConfig {

    /** minio 服务地址 http://ip:port */
    private String url;

    /** 用户名 */
    private String accessKey;

    /** 密码 */
    private String secretKey;

    /** 桶名称 */
    private String bucketName;

}

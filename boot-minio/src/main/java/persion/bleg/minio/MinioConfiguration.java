package persion.bleg.minio;

import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import persion.bleg.minio.my.MyMinioClient;

/**
 * @author shiyuquan
 * @since 2020/4/17 2:59 下午
 */
@Slf4j
@Configuration
public class MinioConfiguration {

    @Bean
    @ConditionalOnMissingBean(MyMinioClient.class)
    MyMinioClient minioClient(MinioConfig properties) throws InvalidPortException, InvalidEndpointException {
        return new MyMinioClient(properties.getUrl(), properties.getAccessKey(), properties.getSecretKey());
    }
}

package persion.bleg.mybatis.mp;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import persion.bleg.mybatis.mp.interceptor.ExcuteSqlInterceptor;

/**
 * @author shiyuquan
 * @since 2020/7/22 3:20 下午
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> {
            configuration.addInterceptor(new PaginationInterceptor());
            configuration.addInterceptor(new ExcuteSqlInterceptor());
        };
    }
}

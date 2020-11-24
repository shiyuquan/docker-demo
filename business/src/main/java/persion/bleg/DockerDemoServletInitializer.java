package persion.bleg;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * war 包打包入口
 *
 * @author shiyuquan
 * @since 2020/9/2 3:01 下午
 */
public class DockerDemoServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DockerDemoApplication.class);
    }

}

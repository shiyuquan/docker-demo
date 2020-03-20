package persion.bleg.dockerdemo.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

/**
 * @author shiyuquan
 * @since 2020/3/19 1:35 下午
 */
@Component
@Order(LOWEST_PRECEDENCE - 10)
public class MyApplicationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MyApplicationRunner.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.err.println("do something");
    }
}

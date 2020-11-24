package persion.bleg.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

/**
 * @author shiyuquan
 * @since 2020/3/19 1:35 下午
 */
@Slf4j
@Component
@Order(LOWEST_PRECEDENCE - 10)
public class MyApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("do something after app start");
    }
}

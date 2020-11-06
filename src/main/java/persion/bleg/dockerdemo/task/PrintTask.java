package persion.bleg.dockerdemo.task;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * 打印任务
 *
 * @author shiyuquan
 * @since 2020/5/28 1:45 下午
 */
// @Configuration
// @EnableScheduling
public class PrintTask {

    @Scheduled(fixedDelay = 5000)
    public void printTask() {
        System.err.println("ddd");
        System.err.println(System.currentTimeMillis());
    }
}

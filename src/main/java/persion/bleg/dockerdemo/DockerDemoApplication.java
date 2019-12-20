package persion.bleg.dockerdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class DockerDemoApplication {

    private static final Logger log = LoggerFactory.getLogger(DockerDemoApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(DockerDemoApplication.class, args);

        log.debug("debug");

        log.info("info");

        log.error("error");

        log.warn("warn");

        log.trace("trace");

    }

}

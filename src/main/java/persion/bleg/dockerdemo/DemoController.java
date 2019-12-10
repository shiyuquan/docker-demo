package persion.bleg.dockerdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shiyuquan
 * @since 3:59 下午
 */
@RestController
public class DemoController {

    @GetMapping(value = "test")
    public String test() {
        return "success";
    }
}

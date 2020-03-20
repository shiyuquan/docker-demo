package persion.bleg.dockerdemo.encryptbody.annotation;

import java.lang.annotation.*;

/**
 * @author shiyuquan
 * @since 2020/3/2 12:20 下午
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DecryptBody {

    /**
     * AES DES RSA SHA MD5
     */
    String type() default "AES";
}

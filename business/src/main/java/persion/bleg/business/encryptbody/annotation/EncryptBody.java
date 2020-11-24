package persion.bleg.business.encryptbody.annotation;

import java.lang.annotation.*;

/**
 * @author shiyuquan
 * @since 2020/3/2 12:21 下午
 */
@Target(value = {ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptBody {

    /**
     * AES DES RSA SHA MD5
     */
    String type() default "AES";

}

package persion.bleg.dockerdemo.encryptbody.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import persion.bleg.dockerdemo.constants.SHAType;

/**
 * @author shiyuquan
 * @since 2020/3/2 12:16 下午
 */
@ConfigurationProperties(prefix = "encrypt")
@Configuration
@Data
public class SecretKeyConfig{

    /** rsa 私钥 */
    private String rsaPrivateKey;

    /** rsa 公钥 */
    private String rsaPublicKey;

    /** 编码格式 */
    private String charset = "UTF-8";

    /** 是否开启接口加解密功能 */
    private boolean open = true;

    /** 打印日志开关 */
    private boolean showLog = false;

    /** aes key */
    private String aesKey;

    /** aes 偏移量 */
    private String aesVi;

    /** des key */
    private String desKey;

    /** {@link SHAType} */
    private String shaType;

}
package persion.bleg.business.encryptbody.advice;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import persion.bleg.business.encryptbody.annotation.EncryptBody;
import persion.bleg.business.encryptbody.config.SecretKeyConfig;
import persion.bleg.util.*;

/**
 * @author shiyuquan
 * @since 2020/3/2 12:29 下午
 */
@Slf4j
@ControllerAdvice
public class EncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private boolean encrypt;

    private SecretKeyConfig secretKeyConfig;

    @Autowired
    public void setSecretKeyConfig(SecretKeyConfig secretKeyConfig) {
        this.secretKeyConfig = secretKeyConfig;
    }

    private static ThreadLocal<Boolean> encryptLocal = new ThreadLocal<>();

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        encrypt = false;
        if (returnType.getMethod().isAnnotationPresent(EncryptBody.class) && secretKeyConfig.isOpen()) {
            encrypt = true;
        }
        return encrypt;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        Boolean status = encryptLocal.get();
        if (null != status && !status) {
            encryptLocal.remove();
            return body;
        }
        if (encrypt) {
            EncryptBody encryptBody = returnType.getMethodAnnotation(EncryptBody.class);
            String encryType = encryptBody.type();

            response.getHeaders().add("encryption", "1");
            String content = JSON.toJSONString(body);

            return decode(content, encryType);

            // String publicKey = secretKeyConfig.getPublicKey();
            // String content = JSON.toJSONString(body);
            // if (!StringUtils.hasText(publicKey)) {
            //     throw new NullPointerException("Please configure rsa.encrypt.privatekeyc parameter!");
            // }
            // try {
            //     //传入明文和公钥加密,得到密文
            //     result = RSAUtils.publicEncrypt(content, RSAUtils.getPublicKey(publicKey));
            //     if(secretKeyConfig.isShowLog()) {
            //         log.info("Pre-encrypted data：{}，After encryption：{}", content, result);
            //     }
            //     return result;
            // } catch (Exception e) {
            //     log.error("Encrypted data exception", e);
            // }
        }
        return body;
    }


    private String decode(String data, String tpye) {
        String result;
        switch (tpye) {
            case "RSA":
                String publicKey = secretKeyConfig.getRsaPublicKey();
                if (!StringUtils.hasText(publicKey)) {
                    throw new NullPointerException("Please configure rsa.encrypt.privatekeyc parameter!");
                }
                try {
                    //传入明文和公钥加密,得到密文
                    result = RSAUtils.publicEncrypt(data, RSAUtils.getPublicKey(publicKey));
                    if(secretKeyConfig.isShowLog()) {
                        log.info("Pre-encrypted data：{}，After encryption：{}", data, result);
                    }
                    return result;
                } catch (Exception e) {
                    log.error("Encrypted data exception", e);
                }
            case "AES":
                return AESUtils.encrypt(data, secretKeyConfig.getAesKey());
            case "DES":
                return DESUtils.encrypt(data, secretKeyConfig.getDesKey());
            case "MD5":
                return MD5Utils.encrypt(data);
            case "SHA":
                return SHAUtils.encrypt(data, secretKeyConfig.getShaType());
            default:
                return "";
        }
    }
}

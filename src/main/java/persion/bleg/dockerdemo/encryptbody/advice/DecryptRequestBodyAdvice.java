package persion.bleg.dockerdemo.encryptbody.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import persion.bleg.dockerdemo.encryptbody.annotation.DecryptBody;
import persion.bleg.dockerdemo.encryptbody.config.SecretKeyConfig;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author shiyuquan
 * @since 2020/3/2 12:26 下午
 */
@Slf4j
@ControllerAdvice
public class DecryptRequestBodyAdvice implements RequestBodyAdvice {

    /** 默认不解密 */
    private boolean decrypt;

    private SecretKeyConfig secretKeyConfig;

    private HttpServletRequest request;

    @Autowired
    public void setSecretKeyConfig(SecretKeyConfig secretKeyConfig) {
        this.secretKeyConfig = secretKeyConfig;
    }

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        decrypt = false;
        String header = request.getHeader("encryption");

        if (secretKeyConfig.isOpen() && "1".equals(header)) {
            Annotation[] annotations = methodParameter.getDeclaringClass().getAnnotations();
            if (annotations != null && annotations.length > 0) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof DecryptBody) {
                        decrypt = true;
                        return decrypt;
                    }
                }
            }
            if (methodParameter.getMethod().isAnnotationPresent(DecryptBody.class)) {
                decrypt = true;
            }
        }
        return decrypt;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType){
        if (decrypt) {
            DecryptBody decryptBody = parameter.getMethodAnnotation(DecryptBody.class);
            String type = decryptBody.type();
            try {
                return new DecryptHttpInputMessage(inputMessage, secretKeyConfig, type);
            } catch (Exception e) {
                throw new  RuntimeException("Decryption failed");
            }
        }
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

}

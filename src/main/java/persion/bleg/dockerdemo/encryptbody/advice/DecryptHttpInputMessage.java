package persion.bleg.dockerdemo.encryptbody.advice;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.util.StringUtils;
import persion.bleg.dockerdemo.encryptbody.config.SecretKeyConfig;
import persion.bleg.dockerdemo.encryptbody.util.EncryptUtils;
import persion.bleg.dockerdemo.util.RSAUtils;

/**
 * @author shiyuquan
 * @since 2020/3/2 12:22 下午
 */
@Slf4j
public class DecryptHttpInputMessage implements HttpInputMessage {

    private HttpHeaders headers;
    private InputStream body;

    private SecretKeyConfig secretKeyConfig;

    public DecryptHttpInputMessage(HttpInputMessage inputMessage, String privateKey, String charset, boolean showLog) throws Exception {

        if (StringUtils.isEmpty(privateKey)) {
            throw new IllegalArgumentException("privateKey is null");
        }

        this.headers = inputMessage.getHeaders();
        String content = new BufferedReader(new InputStreamReader(inputMessage.getBody()))
                .lines().collect(Collectors.joining(System.lineSeparator()));
        String decryptBody;
        //传入密文和私钥,得到明文
        decryptBody = RSAUtils.privateDecrypt(content, RSAUtils.getPrivateKey(privateKey));
        this.body = new ByteArrayInputStream(decryptBody.getBytes());
    }

    public DecryptHttpInputMessage(HttpInputMessage inputMessage, SecretKeyConfig secretKeyConfig, String type) throws Exception {
        this.secretKeyConfig = secretKeyConfig;
        this.headers = inputMessage.getHeaders();
        String content = new BufferedReader(new InputStreamReader(inputMessage.getBody()))
                .lines().collect(Collectors.joining(System.lineSeparator()));
        String decryptBody;
        //传入密文和私钥,得到明文
        decryptBody = EncryptUtils.decode(this.secretKeyConfig, content, type);
        this.body = new ByteArrayInputStream(decryptBody.getBytes());
    }

    @Override
    public InputStream getBody(){
        return body;
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }


}

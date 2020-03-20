package persion.bleg.dockerdemo.encryptbody.util;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import persion.bleg.dockerdemo.encryptbody.annotation.EncryptBody;
import persion.bleg.dockerdemo.encryptbody.config.SecretKeyConfig;
import persion.bleg.dockerdemo.util.*;

/**
 * @author shiyuquan
 * @since 2020/3/19 9:51 上午
 */
public class EncryptUtils {

    private static final Logger log = LoggerFactory.getLogger(EncryptUtils.class);

    private EncryptUtils() {}

    public static String decode(SecretKeyConfig secretKeyConfig, String data, String type) {
        String result;
        switch (type) {
            case "RSA":
                String pk = secretKeyConfig.getRsaPrivateKey();
                if (StringUtils.isEmpty(pk)) {
                    throw new RuntimeException("rsa private key not be null!");
                }
                try {
                    result = RSAUtils.privateDecrypt(data, RSAUtils.getPrivateKey(pk));
                    if (secretKeyConfig.isShowLog()) {
                        log.info("pre data: {}, after data: {}", data, result);
                    }
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException("decode failed!");
                }
            case "AES":
                String aesKey = secretKeyConfig.getAesKey();
                String aesVi = secretKeyConfig.getAesVi();
                if (StringUtils.isEmpty(aesKey)) {
                    throw new RuntimeException("please config aes key");
                }
                return AESUtils.decrypt(data, aesKey, aesVi);
            case "DES":
                String desKey = secretKeyConfig.getDesKey();
                if (StringUtils.isEmpty(desKey)) {
                    throw new RuntimeException("please config des key");
                }
                return DESUtils.decrypt(new String(Base64.decodeBase64(data)), desKey);
            default:
                throw new RuntimeException("unsupport decode type!");
        }
    }

    public static String encode(SecretKeyConfig secretKeyConfig, String data, String type) {
        String result;
        switch (type) {
            case "RSA":
                String pk = secretKeyConfig.getRsaPublicKey();
                if (StringUtils.isEmpty(pk)) {
                    throw new RuntimeException("rsa public key not be null!");
                }
                try {
                    result = RSAUtils.publicEncrypt(data, RSAUtils.getPublicKey(pk));
                    if (secretKeyConfig.isShowLog()) {
                        log.info("pre data: {}, after data: {}", data, result);
                    }
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException("encrypt failed!");
                }
            case "AES":
                String aesKey = secretKeyConfig.getAesKey();
                String aesVi = secretKeyConfig.getAesVi();
                if (StringUtils.isEmpty(aesKey)) {
                    throw new RuntimeException("please config aes key");
                }
                if (StringUtils.isEmpty(aesVi)) {
                    throw new RuntimeException("please config aes vi");
                }

                return new String(AESUtils.encrypt(data, aesKey, aesVi));
            case "DES":
                String desKey = secretKeyConfig.getDesKey();
                if (StringUtils.isEmpty(desKey)) {
                    throw new RuntimeException("please config des key");
                }
                return new String(Base64.encodeBase64(DESUtils.encrypt(data, desKey).getBytes()));
            case "MD5":
                return MD5Utils.encrypt(data);
            case "SHA":
                return SHAUtils.encrypt(data, secretKeyConfig.getShaType());
            default:
                throw new RuntimeException("unsupport decode type!");
        }
    }
}

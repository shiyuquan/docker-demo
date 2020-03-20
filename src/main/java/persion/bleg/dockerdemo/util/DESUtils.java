package persion.bleg.dockerdemo.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;

/**
 * @author shiyuquan
 * @since 2020/3/9 12:35 下午
 */
public class DESUtils {

    private DESUtils() {}

    /**
     * DES加密
     * @param content  字符串内容
     * @param password 密钥
     */
    public static String encrypt(String content, String password){
        return des(content,password,Cipher.ENCRYPT_MODE);
    }


    /**
     * DES解密
     * @param content  字符串内容
     * @param password 密钥
     */
    public static String decrypt(String content, String password){
        return des(content,password, Cipher.DECRYPT_MODE);
    }


    /**
     * DES加密/解密公共方法
     * @param content  字符串内容
     * @param password 密钥
     * @param type     加密：{@link Cipher#ENCRYPT_MODE}，解密：{@link Cipher#DECRYPT_MODE}
     */
    private static String des(String content, String password, int type) {
        try {
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(password.getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(type, keyFactory.generateSecret(desKey), random);

            if (type == Cipher.ENCRYPT_MODE) {
                byte[] byteContent = content.getBytes("utf-8");
                return Hex2Utils.parseByte2HexStr(cipher.doFinal(byteContent));
            } else {
                byte[] byteContent = Hex2Utils.parseHexStr2Byte(content);
                assert byteContent != null;
                return new String(cipher.doFinal(byteContent));
            }
        } catch (Exception e) {
            throw new RuntimeException("des failed");
        }
    }

    public static void main(String[] args) {
        System.err.println("{ \n" +
                "\tcode: " + 1 + ",\n" +
                "\tmsg: " + 1 + ",\n" +
                "\tdata: " + 1 + "\n" +
                "}");
    }

}

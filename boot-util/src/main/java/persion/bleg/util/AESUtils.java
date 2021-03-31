package persion.bleg.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @author shiyuquan
 * @since 2020/3/9 12:33 下午
 */
public class AESUtils {

    private AESUtils() {}

    /**
     * AES加密
     *
     * @param content  字符串内容
     * @param password 密钥
     */
    public static String encrypt(String content, String password) {
        return aes(content, password, Cipher.ENCRYPT_MODE);
    }


    /**
     * AES解密
     *
     * @param content  字符串内容
     * @param password 密钥
     */
    public static String decrypt(String content, String password) {
        return aes(content, password, Cipher.DECRYPT_MODE);
    }

    /**
     * AES加密/解密 公共方法
     *
     * @param content  字符串
     * @param password 密钥
     * @param type     加密：{@link Cipher#ENCRYPT_MODE}，解密：{@link Cipher#DECRYPT_MODE}
     */
    private static String aes(String content, String password, int type) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password.getBytes());
            generator.init(128, random);
            SecretKey secretKey = generator.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(type, key);
            if (type == Cipher.ENCRYPT_MODE) {
                byte[] byteContent = content.getBytes("utf-8");
                return Hex2Utils.parseByte2HexStr(cipher.doFinal(byteContent));
            } else {
                byte[] byteContent = Hex2Utils.parseHexStr2Byte(content);
                return new String(cipher.doFinal(byteContent));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** ------------------------------------------------------*/


    /**
     * 加密方法
     *
     * @param data 要加密的数据
     * @param key  加密key
     * @param iv   加密iv
     * @return 加密的结果
     */
    public static byte[] encrypt(String data, String key, String iv) {
        try {

            //"算法/模式/补码方式"NoPadding PkcsPadding
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            int blockSize = cipher.getBlockSize();

            byte[] dataBytes = data.getBytes();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }

            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);

            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(plaintext);

            return Base64.getEncoder().encode(encrypted);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解密方法
     *
     * @param data 要解密的数据
     * @param key  解密key
     * @param iv   解密iv
     * @return 解密的结果
     */
    public static String decrypt(String data, String key, String iv) {
        try {
            byte[] encrypted1 = Base64.getDecoder().decode(data);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original);
            return originalString;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 加密方法 ECB
     *
     * @param data 要加密的数据
     * @param key  加密key
     * @return 加密的结果
     */
    public static byte[] encryptECB(String data, String key) {
        try {

            //"算法/模式/补码方式"NoPadding PkcsPadding
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            int blockSize = cipher.getBlockSize();

            byte[] dataBytes = data.getBytes();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }

            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
            SecretKeySpec keyspec = getKey(key.getBytes());

            cipher.init(Cipher.ENCRYPT_MODE, keyspec);
            byte[] encrypted = cipher.doFinal(plaintext);

            return Base64.getEncoder().encode(encrypted);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解密方法 ECB
     *
     * @param data 要解密的数据
     * @param key  解密key
     * @return 解密的结果
     */
    public static String decryptEB(String data, String key) {
        try {
            byte[] encrypted1 = Base64.getDecoder().decode(data);

            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec keyspec = getKey(key.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, keyspec);
            byte[] original = cipher.doFinal(encrypted1);
            return new String(original).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取16位的密码
     *
     * @param keyb 密码字节数组
     */
    public static SecretKeySpec getKey(byte[] keyb) {
        byte[] keyByte = keyb;
        // 创建一个空的八位数组,默认情况下为0
        byte[] byteTemp = new byte[16];
        // 将用户指定的规则转换成八位数组
        int i = 0;
        for (; i < byteTemp.length && i < keyByte.length; i++) {
            byteTemp[i] = keyByte[i];
        }
        return new SecretKeySpec(byteTemp, "AES");
    }

    public static void main(String[] args) throws Exception {
        String data = "31011111111";
        String key = "HZ20200307";
        String encodeData = new String(encryptECB(data, key));
        System.err.println(encodeData);
        System.err.println(decryptEB(encodeData, key));
    }

}

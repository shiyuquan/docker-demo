package persion.bleg.dockerdemo.util;

import persion.bleg.dockerdemo.constants.SHAType;
import sun.security.provider.SHA;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author shiyuquan
 * @since 2020/3/9 12:38 下午
 */
public class SHAUtils {

    private SHAUtils() {}

    /**
     * SHA加密公共方法
     * @param string 目标字符串
     * @param shaType   加密类型 {@link SHAType}
     */
    public static String encrypt(String string, String shaType) {
        SHAType type = SHAType.getByValue(shaType);
        if (string == null || "".equals(string.trim())) {
            return "";
        }
        if (type == null) {
            type = SHAType.SHA256;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance(type.value);
            byte[] bytes = md5.digest((string).getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}

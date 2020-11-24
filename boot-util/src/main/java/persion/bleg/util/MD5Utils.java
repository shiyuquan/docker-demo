package persion.bleg.util;

import java.io.InputStream;
import java.security.MessageDigest;

/**
 * @author shiyuquan
 * @since 2020/3/9 12:37 下午
 */
public class MD5Utils {

    /**
     * MD5加密-32位小写
     * */
    public static String encrypt(String encryptStr) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] md5Bytes = md5.digest(encryptStr.getBytes());
            StringBuilder hexValue = new StringBuilder();
            for (byte md5Byte : md5Bytes) {
                int val = ((int) md5Byte) & 0xff;
                if (val < 16) {
                    hexValue.append("0");
                }
                hexValue.append(Integer.toHexString(val));
            }
            encryptStr = hexValue.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return encryptStr;
    }

    /**
     * 给定输入流，返回md5
     *
     * @param inputStream 文件
     * @return md5
     */
    public static String createMd5(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        //生成MD5实例
        MessageDigest md5 = null;
        int available = 0;
        try {
            md5 = MessageDigest.getInstance("MD5");
            available = inputStream.available();
        } catch (Exception e) {
            throw new RuntimeException("流读取出现问题", e);
        }
        byte[] bytes = new byte[available];
        md5.update(bytes);
        for (byte by : md5.digest()) {
            //将生成的字节MD5值转换成字符串
            sb.append(String.format("%02X", by));
        }

        return sb.toString();
    }
}

package persion.bleg.util;

import sun.misc.BASE64Decoder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author shiyuquan
 * @since 2020/8/31 11:37 上午
 */
public class ImageUtil {

    /**
     * base64字符串转化成图片
     *
     * @param imgData     图片编码
     * @param imgFilePath 存放到本地路径
     * @return
     * @throws IOException
     */
    public static boolean generateImage(String imgData, String imgFilePath) {
        // 图像数据为空
        if (imgData == null) {
            return false;
        }
        BASE64Decoder decoder = new BASE64Decoder();
        try (OutputStream out = new FileOutputStream(imgFilePath);) {
            // Base64解码
            byte[] b = decoder.decodeBuffer(imgData);
            for (int i = 0; i < b.length; ++i) {
                // 调整异常数据
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
            out.write(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void main(String[] args) {
        String data ="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";
        generateImage(data, "/Users/shiyuquan/Documents/aaaaa.jpg");
    }
}
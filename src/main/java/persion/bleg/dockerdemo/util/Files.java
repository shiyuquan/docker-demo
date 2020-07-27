package persion.bleg.dockerdemo.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import persion.bleg.dockerdemo.base.BlegException;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;
import java.util.Vector;

/**
 * @author shiyuquan
 * @since 2020/5/7 3:08 下午
 */
@Slf4j
public class Files {

    /**
     * 切割文件，输出到制定文件夹
     *
     * @param srcFile   要切割的文件
     * @param outputDir 输出的文件夹
     * @param size      切割的大小
     */
    public static void split(File srcFile, String outputDir, int size) {
        try (FileInputStream fis = FileUtils.openInputStream(srcFile)) {
            File temp = null;
            byte[] buff = new byte[size];
            int len = 0;
            int i = 0;
            while ((len = IOUtils.read(fis, buff)) > 0) {
                temp = FileUtils.getFile(outputDir, String.valueOf(i));
                FileUtils.writeByteArrayToFile(temp, buff, 0, len);
                i++;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        }
    }

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     */
    public static void newFolder(String path) {
        File file = new File(path);
        if (!file.exists() && !file.isDirectory()) {
            //创建文件夹
            file.mkdir();
        }
    }

    /**
     * 给定字节数组和文件路径，名称，创建文件
     *
     * @param bytes    字节数组
     * @param path     文件路径
     * @param fileName 文件名称
     */
    public static File saveFile(byte[] bytes, String path, String fileName) {
        newFolder(path);
        String fullName = path + File.separator + fileName;
        File file = new File(fullName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
        } catch (Exception e) {
            log.error("创建文件 {} 失败", fullName);
            throw new BlegException(500, "文件创建失败");
        }
        return file;
    }

    /**
     * 给定字节数组和文件路径，名称，创建文件
     *
     * @param is       输入流
     * @param path     文件路径
     * @param fileName 文件名称
     */
    public static void saveFile(InputStream is, String path, String fileName) {
        newFolder(path);
        String fullName = path + File.separator + fileName;
        File file = new File(fullName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            // 文件拷贝
            byte[] flush = new byte[1024];
            int len = -1;
            while ((len = is.read(flush)) != -1) {
                fos.write(flush, 0, len);
                fos.flush();
            }
        } catch (Exception e) {
            log.error("创建文件 {} 失败", fullName);
            throw new BlegException(500, "文件创建失败");
        }
    }

    /**
     * 删除文件夹
     *
     * @param dirPath 文件夹路径
     */
    public static void deleteDir(String dirPath) {
        File file = new File(dirPath);
        // 判断是否是文件
        if (file.isFile()) {
            // 删除
            file.delete();
        } else {
            // 获取文件
            File[] files = file.listFiles();
            if (files == null) {
                // 删除
                file.delete();
            } else {
                // 循环
                for (int i = 0; i < files.length; i++) {
                    deleteDir(files[i].getAbsolutePath());
                }
                // 删除
                file.delete();
            }
        }

    }

    /**
     * 合并文件
     *
     * @param destPath  合并出来的文件
     * @param chunkPath 文件碎片的路径
     * @throws IOException
     */
    public static void merge(String destPath, List<String> chunkPath) {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(destPath, true))) {

            Vector<InputStream> vi = new Vector<>();
            // 输入流
            for (int i = 0; i < chunkPath.size(); i++) {
                vi.add(new BufferedInputStream(new FileInputStream(chunkPath.get(i))));
            }
            SequenceInputStream sis = new SequenceInputStream(vi.elements());
            // 文件拷贝
            byte[] flush = new byte[1024];
            int len = -1;
            while ((len = sis.read(flush)) != -1) {
                os.write(flush, 0, len);
            }

            // 释放资源
            os.flush();
            sis.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BlegException(500, "合并文件失败");
        }
    }

    /**
     * 下载文件，给定文件和response
     *
     * @param file     文件
     * @param response response
     */
    public static void download(File file, HttpServletResponse response) {
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            download(is, response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BlegException(500, "下载失败");
        }
    }

    /**
     * 通用文件下载方法，给定输入流和response
     *
     * @param is       输入流
     * @param response response
     */
    public static void download(InputStream is, HttpServletResponse response) {
        try {
            byte[] buff = new byte[1024];
            BufferedInputStream bis = new BufferedInputStream(is);
            OutputStream os = response.getOutputStream();
            int i = bis.read(buff);
            while (i != -1) {
                os.write(buff, 0, buff.length);
                os.flush();
                i = bis.read(buff);
            }
            os.close();
            bis.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BlegException(500, "下载失败");
        }
    }

    /**
     * 获取文件后缀
     *
     * @param fileName 文件名
     * @return 后缀
     */
    public static String getSuffix(String fileName) {
        String[] arr = fileName.split("\\.");
        if (arr.length <= 1) {
            return "";
        }
        return arr[arr.length - 1];
    }

    /**
     * url 编码字符串
     *
     * @param str 字符串
     * @return 编码后字符串
     */
    public static String urlEbcode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
            throw new BlegException(500, "不支持该浏览器解码");
        }
    }

    public static void main(String[] args) throws Exception {
        System.err.println(System.currentTimeMillis() + "" + (Math.random()*9+1)*1000);
        System.err.println((int) ((Math.random()*9+1)*1000000));
        System.err.println(System.currentTimeMillis() + "" + (int) ((Math.random()*9+1)*1000000));
    }

}

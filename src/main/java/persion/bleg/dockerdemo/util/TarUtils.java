package persion.bleg.dockerdemo.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import persion.bleg.dockerdemo.base.BlegException;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * @author shiyuquan
 * @since 2020/7/1 1:09 下午
 */
@Slf4j
public class TarUtils {

    private TarUtils() {}

    /**
     * 将文件打包成.tar 问价
     * @param destFileName 输出的文件
     * @param files 打包的文件
     */
    public static void tarFiles(String destFileName, File... files) {
        File destFile = new File(destFileName);

        try (FileOutputStream fileOutputStream = new FileOutputStream(destFile);
             BufferedOutputStream bufferedWriter = new BufferedOutputStream(fileOutputStream);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(bufferedWriter)) {

            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            for (File file : files) {
                addTarArchiveEntryToTarArchiveOutputStream(file, tar, "");
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new BlegException("打包文件失败");
        }
    }

    private static void addTarArchiveEntryToTarArchiveOutputStream(File file, TarArchiveOutputStream tar, String prefix) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(file, prefix + File.separator + file.getName());

        if (file.isFile()) {
            entry.setSize(file.length());
            tar.putArchiveEntry(entry);
            try (FileInputStream fileInputStream = new FileInputStream(file); BufferedInputStream input = new BufferedInputStream(fileInputStream);) {
                IOUtils.copy(input, tar);
            }
            tar.closeArchiveEntry();
        } else {
            tar.putArchiveEntry(entry);
            tar.closeArchiveEntry();
            prefix += File.separator + file.getName();
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    addTarArchiveEntryToTarArchiveOutputStream(f, tar, prefix);
                }
            }
        }
    }

    /**
     * 将文件压缩为.gz格式
     *
     * @param fileName 文件
     */
    public static void gzip(String fileName) {
        String outFileName = fileName + ".gz";
        try (FileOutputStream fos = new FileOutputStream(outFileName);
             GZIPOutputStream gos = new GZIPOutputStream(fos);
             FileInputStream fis = new FileInputStream(fileName)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) != -1) {
                gos.write(buf, 0, len);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BlegException("压缩文件失败");
        }
    }

    /**
     * 将文件打包成压缩文件.tar.gz格式
     * @param fileName 文件
     */
    public static void tarGzFile(String fileName) {
        File f = new File(fileName);
        String destFileName = fileName + ".tar";
        File[] fs = f.listFiles();
        if (null != fs) {
            tarFiles(destFileName, fs);
            gzip(destFileName);
        }
    }

}

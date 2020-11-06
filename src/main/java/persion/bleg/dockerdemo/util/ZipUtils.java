package persion.bleg.dockerdemo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import persion.bleg.dockerdemo.base.BlegException;

import java.io.*;
import java.util.zip.*;

/**
 * @author shiyuquan
 * @since 2020/7/1 11:02 上午
 */
@Slf4j
public class ZipUtils {

    public static void unzip(String zip, String destPath) {
        if (StringUtils.isEmpty(zip) || StringUtils.isEmpty(destPath)) {
            throw new BlegException("参数不可为空");
        }
        File zipFile = new File(zip);
        if (!zipFile.exists()) {
            throw new BlegException("zip文件不存在");
        }
        File destFile = new File(destPath);
        if (!destFile.exists()) {
            if (!destFile.mkdirs()) {
                throw new BlegException("输出文件夹创建失败");
            }
        }
        ZipInputStream zis = null;
        BufferedOutputStream bos = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 获取解压文件对绝对路径
                String filePath = destPath + File.separator + entry.getName();
                if (entry.isDirectory()) {
                    new File(filePath).mkdirs();
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buff = new byte[1024];
                    int count;
                    while ((count = zis.read(buff)) != -1) {
                        baos.write(buff, 0, count);
                    }
                    byte[] bytes = baos.toByteArray();
                    File entryFile = new File(filePath);
                    // 创建父目录
                    if (!entryFile.getParentFile().exists()) {
                        if (!entryFile.getParentFile().mkdirs()) {
                            throw new BlegException("解压过程失败");
                        }
                    }
                    bos = new BufferedOutputStream(new FileOutputStream(entryFile));
                    bos.write(bytes);
                    bos.flush();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BlegException("解压问价失败");
        } finally {
            closeStream(zis);
            closeStream(bos);
        }
    }

    /**
     * 压缩文件
     * @param file 文件
     * @param distPath 目标文件夹
     */
    public static void zip(File file, String distPath) {
        if (StringUtils.isEmpty(distPath)) {
            distPath = file.getPath() + File.separator + file.getName() + ".zip";
        }
        File destFile = new File(distPath);
        if (destFile.exists()) {
            if (!destFile.delete()) {
                throw new BlegException(500, "目标文件存在且不可删除");
            }
        }

        try(FileOutputStream fos = new FileOutputStream(destFile);
            CheckedOutputStream cos = new CheckedOutputStream(fos, new CRC32());
            ZipOutputStream zos = new ZipOutputStream(cos)) {
            // CheckedOutputStream 对文件做 CRC32 校验，确保压缩后对zip包含CRC32值
            zos.setLevel(9);
            if (file.isFile()) {
                zipFile("", file, zos);
            } else if (file.isDirectory()) {
                zipFolder("", file, zos);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BlegException(500, "压缩失败");
        }
    }

    public static void zip(String srcPath, String destPath) {
        File f = new File(srcPath);
        if (!f.exists()) {
            throw new BlegException(500, "目标文件不存在");
        }
        zip(f, destPath);
    }

    public static void zip(String path) {
        zip(path, "");
    }

    public static void zip(File file) {
        zip(file, "");
    }

    /**
     * 压缩文件夹
     * @param prefix 前缀
     * @param file 文件
     * @param zos 压缩流
     */
    public static void zipFolder(String prefix, File file, ZipOutputStream zos) {
        String newPrefix = prefix + file.getName() + File.separator;
        File[] files = file.listFiles();
        // 空文件夹
        if (files == null || files.length == 0) {
            zipFile(prefix, file, zos);
        } else {
            for (File file1 : files) {
                if (file.isFile()) {
                    zipFile(newPrefix, file, zos);
                } else if (file.isDirectory()) {
                    zipFolder(newPrefix, file, zos);
                }
            }
        }
    }

    /**
     * zip 压缩单个文件
     *
     * @param prefix 前缀
     * @param file 文件
     * @param zos 压缩流
     */
    public static void zipFile(String prefix, File file, ZipOutputStream zos) {
        String relativePath = prefix + file.getName();
        if (file.isDirectory()) {
            relativePath += File.separator;
        }
        ZipEntry entry = new ZipEntry(relativePath);
        InputStream is = null;
        try {
            zos.putNextEntry(entry);
            if (file.isFile()) {
                is = new FileInputStream(file);
                byte[] buff = new byte[1024];
                int len;
                while ((len = is.read(buff)) != -1) {
                    zos.write(buff, 0, len);
                }
            }
            zos.closeEntry();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BlegException(500, "压缩文件失败");
        } finally {
            closeStream(is);
        }
    }

    private static void closeStream(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException e) {

        }
    }

    public static void main(String[] args) {
        String s = "";
        boolean b = org.apache.commons.lang3.StringUtils.isEmpty(s);
        System.err.println(b);
    }
}

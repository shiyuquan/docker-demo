package persion.bleg.minio;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import persion.bleg.minio.my.MyMinioClient;

/**
 * @author shiyuquan
 * @since 2020/4/17 3:08 下午
 */
@Slf4j
@Component
@AllArgsConstructor
public class MinioService {
    private final MyMinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件
     */
    public String uploadFile(byte[] data, String filePath) {
        // InputStream inputStream = new ByteArrayInputStream(data);
        // String path;
        // try {
        //     minioClient.putObject(minioProperties.getBucketName(), filePath, inputStream, inputStream.available(), getContextType(fileName));
        //     path = filePath;
        // } catch (Exception e) {
        //     log.error("=====minio:" + e.getMessage());
        // }
        // return path;
        return "";
    }


    /**
     * 删除文件
     */
    public boolean delete(String filePath) {
        try {
            minioClient.removeObject(minioConfig.getBucketName(), filePath);
            return true;
        } catch (Exception e) {
            log.error("====minio:删除失败，{}", e.getMessage());
        }
        return false;
    }

    public String getContextType(String path) {
        String suffix = path.substring(path.lastIndexOf(".") + 1);
        String type = "application/octet-stream";
        if (suffix.equals("jpg") || suffix.equals("jpeg") || suffix.equals("png") || suffix.equals("bmp") || suffix.equals("gif")) {
            type = "image/jpeg";
        }
        return type;
    }

}
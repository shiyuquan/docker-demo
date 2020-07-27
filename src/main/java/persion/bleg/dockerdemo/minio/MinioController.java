package persion.bleg.dockerdemo.minio;

import io.minio.ObjectStat;
import io.minio.PutObjectOptions;
import io.minio.messages.Part;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import persion.bleg.dockerdemo.base.BlegException;
import persion.bleg.dockerdemo.base.IResult;
import persion.bleg.dockerdemo.base.Result;
import persion.bleg.dockerdemo.minio.my.MyMinioClient;
import persion.bleg.dockerdemo.util.RedisUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static persion.bleg.dockerdemo.constants.DefalutConstant.DEFAULT_API_PREFIX;

/**
 * @author shiyuquan
 * @since 2020/4/17 3:22 下午
 */
@Slf4j
@Api(tags = "minio接口")
@RestController
@RequestMapping(DEFAULT_API_PREFIX + "/minio")
public class MinioController {

    private static final String MINIO = "redis:minio";

    private MyMinioClient minioClient;
    private MinioConfig minioConfig;
    private RedisTemplate<String, Object> redisTemplate;
    private RedisUtils redisUtils;

    @Autowired
    public void setMinioClient(MyMinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Autowired
    public void setMinioConfig(MinioConfig minioConfig) {
        this.minioConfig = minioConfig;
    }

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setRedisUtils(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    /**
     * 单文件上传到minio服务
     *
     * @param file 文件
     */
    @ApiOperation(value = "上传文件到minio服务")
    @PostMapping(value = "/upload")
    public IResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        long start = System.currentTimeMillis();
        String bucketName = minioConfig.getBucketName();
        Map<String, String> res;
        boolean b;
        try {
            b = minioClient.bucketExists(bucketName);
        } catch (Exception e) {
            throw new BlegException(500, "判断桶存在报错了", e);
        }
        if (!b) {
            log.info("桶{}不存在，尝试创建桶...", bucketName);
            try {
                minioClient.makeBucket(bucketName);
            } catch (Exception e) {
                throw new BlegException(500, "创建桶失败了", e);
            }
            log.info("桶{}创建成功！", bucketName);
        }

        //得到文件流
        InputStream is;
        long size;
        long part = 5 * 1024 * 1024L;
        try {
            is = file.getInputStream();
            size = is.available();
        } catch (IOException e) {
            throw new BlegException(500, "流读取失败", e);
        }
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        //把文件放置Minio桶(文件夹)
        PutObjectOptions ops = new PutObjectOptions(size, part);
        ops.setContentType(contentType);
        try {
            minioClient.putObject(minioConfig.getBucketName(), fileName, is, ops);
            ObjectStat statObject = minioClient.statObject(minioConfig.getBucketName(), fileName);
            res = getObjectInfo(statObject, minioConfig.getBucketName());
        } catch (Exception e) {
            throw new BlegException(500, "上传失败", e);
        }
        long end = System.currentTimeMillis();
        log.info("time: {}", end - start);
        return new Result<Map<String, String>>().success(res);
    }

    /**
     * 下载minio服务的文件
     * 该方法不如直接用文件链接下载的快
     */
    @ApiOperation(value = "下载minio服务的文件")
    @PostMapping(value = "download/{fileName}")
    public void download(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        long start = System.currentTimeMillis();
        try {
            InputStream fileInputStream = minioClient.getObject(minioConfig.getBucketName(), fileName);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.setContentType("application/force-download");
            response.setCharacterEncoding("UTF-8");
            IOUtils.copy(fileInputStream, response.getOutputStream());
        } catch (Exception e) {
            throw new BlegException(500, "下载失败");
        }
        long end = System.currentTimeMillis();
        log.info("time: {}", end - start);
    }

    /**
     * 删除文件
     */
    @ApiOperation(value = "删除文件")
    @DeleteMapping(value = "/delete/{fileName}")
    public IResult<String> delete(@PathVariable("fileName") String fileName) {
        try {
            minioClient.removeObject(minioConfig.getBucketName(), fileName);
        } catch (Exception e) {
            throw new BlegException(500, "删除失败", e);
        }
        return new Result<String>().success("删除成功");
    }

    /**
     * 通过文件名获取链接
     *
     * @param fileName 文件名
     * @return 链接
     */
    @ApiOperation(value = "通过文件名获取链接")
    @GetMapping(value = "/link/{fileName}")
    public IResult<String> getLink(@PathVariable("fileName") String fileName) {
        String link;
        try {
            link = minioClient.presignedGetObject(minioConfig.getBucketName(), fileName);
        } catch (Exception e) {
            throw new BlegException(500, "获取链接失败", e);
        }
        return new Result<String>().success(link);
    }


    /**
     * 分片上传初始化，根据文件内容md5值生成唯一的上传标识，用于分片上传
     * @param fileName 文件md5值
     * @return upload id
     */
    @ApiOperation(value = "分片上传初始化，根据文件内容md5值生成唯一的上传标识，用于分片上传")
    @PostMapping("/chunk/init")
    public IResult<Map<String, Object>> chunkInit(@RequestParam("fileName") String fileName,
                                                  @RequestParam("contentType") String contentType) {
        long start = System.currentTimeMillis();
        String bucket = minioConfig.getBucketName();
        String ct = contentType;
        if (StringUtils.isEmpty(ct)) {
            ct = "application/octet-stream";
        }
        Map<String, Object> result = new HashMap<String, Object>(16);
        try {
            // 检查文件是否在指定的bucket下已经存在。如果已经存在可以避免重复上传。
            boolean isObjectExists = minioClient.objectExists(bucket, fileName);

            // 获取分片上传需要的uploadId
            if (!isObjectExists) {
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("Content-Type", ct);
                // initiate new multipart upload.
                String uploadId = minioClient.initMultipartUpload(bucket, fileName, headerMap);
                // 将buket 和 fileName 和 uploadId 关联存在redis中，初始化查询redis，
                // 有查到关联说明之前传过了，直接进行之后的操作
                String redisKey = MINIO + ":" + bucket + ":uploadId:" + fileName;

                redisTemplate.opsForValue().set(redisKey, uploadId);
                result.put("uploadId", uploadId);
            } else {
                ObjectStat statObject = minioClient.statObject(bucket, fileName);
                //文件存在，获取文件信息
                Map<String, String> objectInfo = getObjectInfo(statObject, bucket);
                result.putAll(objectInfo);
            }
            // 4. 构建返回结果
            result.put("exist", isObjectExists);
        } catch (Exception e) {
            throw new BlegException(500, "获取分片上传标识错误", e);
        }

        long end = System.currentTimeMillis();

        log.info("time: {}", end - start);
        return new Result<Map<String, Object>>().success(result);
    }

    /**
     * 完成分片文件上传
     *
     * @param uploadId  分片上传初始化的id
     * @param md5  文件的md5
     * @param fileName  完成分片上传文件名称
     * @return 文件信息
     */
    @ApiOperation(value = "完成分片文件上传")
    @PostMapping("/chunk/complete")
    public IResult<Map<String, String>> chunkComplete(@RequestParam("uploadId") String uploadId,
                                                      @RequestParam("md5") String md5,
                                                      @RequestParam("fileName") String fileName) {
        long start = System.currentTimeMillis();
        String bucket = minioConfig.getBucketName();
        Map<String, String> result;
        String redisKey = MINIO + ":" + bucket + ":chunk:" + uploadId;
        String redisKey2 = MINIO + ":" + bucket + ":uploadId:" + md5;

        Set<String> keys = redisTemplate.keys(redisKey + "*");
        List<Object> parts = redisTemplate.opsForValue().multiGet(keys);

        Part[] totalParts = parts.stream()
                .map(o ->  (Part) o)
                .sorted(Comparator.comparing(Part::partNumber))
                .toArray(Part[]::new);
        try {
            // 合并分片
            minioClient.completeMultipart(bucket, md5, uploadId, totalParts);

            // 构建文件上传完成返回的信息，包括文件访问地址等信息
            // 文件的访问地址，直接使用minio文件的访问地址
            ObjectStat statObject = minioClient.statObject(bucket, md5);
            //文件存在，获取文件信息
            result = getObjectInfo(statObject, bucket);
        } catch (Exception e) {
            log.error("文件合并失败", e);
            throw new BlegException(500, "文件合并失败", e);
        }
        redisUtils.delete(redisKey + "*");
        redisUtils.delete(redisKey2);
        long end = System.currentTimeMillis();
        log.info("time: {}", end - start);

        return new Result<Map<String, String>>().success(result);
    }

    /**
     * 配合 前端 simple-uploader 的分片检查接口
     * 单个分片文件上传成功后，会将分片文件的bucket + fileName + md5值 作为key存好
     * 检查key是否存在，存在说明上传过了
     * @return true 上传过了 false 没上传过
     */
    @GetMapping("simpleUploader/chunk/{uploadId}")
    public IResult<Boolean> checkChunk(@PathVariable("uploadId") String uploadId,
                                       Chunk chunk) {
        String bucket = minioConfig.getBucketName();

        String redisKey = MINIO + ":" + bucket + ":chunk:" + uploadId + ":" + chunk.getIdentifier() + ":" + chunk.getChunkNumber();
        return new Result<Boolean>().success(redisTemplate.hasKey(redisKey));
    }

    /**
     * 配合前端上传组件 simple-uploader 的分片上传接口
     * @param uploadId minio的上传接口
     * @param chunk 文件片信息
     * @return string
     */
    @PostMapping("simpleUploader/chunk/{uploadId}")
    public IResult<String> uploadChunk(@PathVariable("uploadId") String uploadId,
                              Chunk chunk) {
        MultipartFile file = chunk.getFile();
        String bucket = minioConfig.getBucketName();
        String etag;
        long start = System.currentTimeMillis();
        String redisKey = MINIO + ":" + bucket + ":chunk:" + uploadId + ":" + chunk.getIdentifier() + ":" + chunk.getChunkNumber();
        Boolean b = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(b)) {
            return new Result<String>().success("文件片已存在");
        }
        int partNumber = chunk.getChunkNumber() + 1;
        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(file.getInputStream());
        } catch (IOException e) {
            log.error("error", e);
            throw new BlegException(500, "文件流转换失败", e);
        }
        try {
            // 执行分片上传
            etag = minioClient.putObject(bucket,
                    chunk.getIdentifier(),
                    bis,
                    chunk.getCurrentChunkSize().intValue(),
                    null,
                    uploadId,
                    partNumber);
            // 保存 part
            Part p = new Part(partNumber, etag);
            redisTemplate.opsForValue().set(redisKey, p);
        } catch (Exception e) {
            log.error("error", e);
            throw new BlegException(500, "上传失败", e);
        }
        long end = System.currentTimeMillis();
        log.info("time: {}", end - start);

        return new Result<String>().success("上传片成功");
    }

    private Map<String, String> getObjectInfo(ObjectStat statObject, String bucket) {
        Map<String, String> result = new HashMap<>();
        result.put("url", minioConfig.getUrl() + "/" + bucket + "/" + statObject.name());
        result.put("storePath", "/" + bucket + "/" + statObject.name());
        result.put("fileName", statObject.name());
        String cd = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(statObject.createdTime());
        result.put("createdTime", cd);
        result.put("length", String.valueOf(statObject.length()));
        return result;
    }

}

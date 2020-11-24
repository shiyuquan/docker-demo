package persion.bleg.fastdfs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import persion.bleg.boot.base.BlegException;
import persion.bleg.boot.base.IResult;
import persion.bleg.boot.base.Result;
import persion.bleg.redis.RedisUtils;
import persion.bleg.util.Files;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author shiyuquan
 * @since 2020/5/9 10:55 上午
 */
@Slf4j
@Api(tags = "fdfs 接口")
@RestController
@RequestMapping("api/v1/fdfs")
public class FastDfsController {

    private static final String FDFS = "redis:fdfs:chunk:";

    private FastDfsUtils fastDFSUtils;
    private RedisTemplate<String, Object> redisTemplate;
    private RedisUtils redisUtils;

    @Autowired
    public void setFastDFSUtils(FastDfsUtils fastDFSUtils) {
        this.fastDFSUtils = fastDFSUtils;
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
     * 上传文件
     * @param file 文件
     * @return
     */
    @ApiOperation(value = "上传文件")
    @PostMapping("/uploadFile")
    public String uploadFile(@RequestParam("file") MultipartFile file) {

        try {
            // 获取原文件名
            String origFileName = file.getOriginalFilename();
            log.info("原始文件名：{}", origFileName);

            // 获取扩展名
            String extName = origFileName.substring(origFileName.lastIndexOf(".") + 1);
            log.info("原始文件扩展名：{}", extName);

            // 获取文件存储路径
            String[] uriArray = fastDFSUtils.uploadFile(file.getBytes(), extName);

            String groupName = uriArray[0];
            String fileId = uriArray[1];

            String uri = groupName + "/" + fileId;
            log.info("返回的文件存储路径：{}", uri);
            return uri;

        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    /**
     * 下载文件
     * @param groupName
     * @param fileId
     * @return
     */
    @ApiOperation(value = "下载文件")
    @PostMapping("/downloadFile")
    public void downloadFile(@RequestParam("groupName") String groupName,
                             @RequestParam("fileId") String fileId,
                             HttpServletResponse response) {

        try {

            // 获取文件名
            int index = fileId.lastIndexOf("/");
            String fileName = fileId.substring(index + 1);

            /**
             * 参数格式：
             * groupName: group1
             * fileId: M00/00/00/wKjlj15o9rGAP5MkAACpl5L2fqw700.jpg
             */
            byte[] fileByte = fastDFSUtils.downloadFile(groupName, fileId);
            InputStream inputStream = new ByteArrayInputStream(fileByte);
            response.setHeader("content-type", "application/octet-stream");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

            byte[] buff = new byte[1024];
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            OutputStream os = response.getOutputStream();
            int i = bis.read(buff);
            while (i != -1) {
                os.write(buff, 0, buff.length);
                os.flush();
                i = bis.read(buff);
            }
            os.close();
            bis.close();
            log.info("Download  successfully!");
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

    }

    /**
     * 删除文件
     * @param groupName
     * @param fileId
     * @return
     */
    @ApiOperation(value = "删除文件")
    @DeleteMapping("/deleteFile")
    public String deleteFile(@RequestParam("groupName") String groupName,
                             @RequestParam("fileId") String fileId) {
        try {
            /**
             * 参数格式：
             * groupName: group1
             * fileId: M00/00/00/wKjlj15o9rGAP5MkAACpl5L2fqw700.jpg
             */
            int i = fastDFSUtils.deleteFile(groupName, fileId);
            return (i > 0) ? "删除文件成功" : "删除文件失败";
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }

    }

    /**
     * md5删除文件
     */
    @ApiOperation(value = "删除文件")
    @DeleteMapping("/deleteMd5File")
    public String deleteFile(@RequestParam("md5") String md5) {
        String redisKey = FDFS + md5;

        Set<String> keys = redisTemplate.keys(redisKey + "*");
        List<Object> parts = redisTemplate.opsForValue().multiGet(keys);
        // 起并行流进行删除
        parts.forEach(o -> {
            String k;
            Part p = (Part) o;
            try {
                log.info("delete part {}", p.getNumber());
                k = redisKey + ":" + p.getNumber();
                fastDFSUtils.deleteFile(p.getGroupName(), p.getFileId());
                redisTemplate.delete(k);
            } catch (Exception e) {
                throw new BlegException(500, "删除失败", e);
            }
        });
        redisUtils.delete(redisKey + "*");
        return "success";
    }

    /**
     * 配合前端上传组件 simple-uploader 的分片上传接口
     * @param chunk 文件片信息
     * @return string
     */
    @PostMapping("simpleUploader/chunk")
    public IResult<String> uploadChunk(Chunk chunk) {
        MultipartFile file = chunk.getFile();
        long start = System.currentTimeMillis();
        String redisKey = FDFS + chunk.getIdentifier() + ":" + chunk.getChunkNumber();
        Boolean b = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(b)) {
            return new Result<String>().success("文件片已存在");
        }
        try {
            // 获取原文件名
            String origFileName = file.getOriginalFilename();
            log.info("原始文件名：{}", origFileName);

            // 获取扩展名
            String extName = origFileName.substring(origFileName.lastIndexOf(".") + 1);
            log.info("原始文件扩展名：{}", extName);

            // 获取文件存储路径
            String[] uriArray = fastDFSUtils.uploadFile(file.getBytes(), extName);
            String groupName = uriArray[0];
            String fileId = uriArray[1];
            String uri = groupName + "/" + fileId;

            Part p = new Part();
            p.setFileId(fileId);
            p.setGroupName(groupName);
            p.setUri(uri);
            p.setNumber(chunk.getChunkNumber());
            p.setOrigFileName(origFileName);
            p.setIdentifier(chunk.getIdentifier());

            // 保存 part
            redisTemplate.opsForValue().set(redisKey, p);
        } catch (Exception e) {
            log.error("error", e);
            throw new BlegException(500, "上传失败", e);
        }
        long end = System.currentTimeMillis();
        log.info("time: {}", end - start);

        return new Result<String>().success("上传片成功");
    }

    /**
     * 配合 前端 simple-uploader 的分片检查接口
     * 单个分片文件上传成功后，会将分片文件的bucket + fileName + md5值 作为key存好
     * 检查key是否存在，存在说明上传过了
     * @return true 上传过了 false 没上传过
     */
    @GetMapping("simpleUploader/chunk")
    public IResult<Map<String, Object>> checkChunk(Chunk chunk) {

        Map<String, Object> res = new HashMap<>();
        List<Integer> uplatedChunk = new ArrayList<>();
        String redisKey = FDFS + chunk.getIdentifier() + ":";
        Set<String> keys = redisTemplate.keys(redisKey + "*");
        boolean needMerge = false;
        if (keys.size() == chunk.getTotalChunks()) {
            needMerge = true;
        } else {
            keys.forEach(o -> {
                String index = o.substring(o.lastIndexOf(":") + 1);
                uplatedChunk.add(new Integer(index));
            });
        }
        res.put("uploaded", uplatedChunk);
        res.put("needMerge", needMerge);
        return new Result<Map<String, Object>>().success(res);
    }

    /**
     * 给定md5， 下载文件
     *
     * @param md5  文件的md5
     * @return 文件信息
     */
    @ApiOperation(value = "完成分片文件上传")
    @GetMapping("simpleUploader/chunk/complete/{md5}")
    public void dowload(@PathVariable("md5") String md5, HttpServletResponse response) {
        long start = System.currentTimeMillis();
        String redisKey = FDFS + md5;

        Set<String> keys = redisTemplate.keys(redisKey + "*");
        List<Object> parts = redisTemplate.opsForValue().multiGet(keys);

        Part[] totalParts = parts.stream()
                .map(o ->  (Part) o)
                .sorted(Comparator.comparing(Part::getNumber))
                .toArray(Part[]::new);

        String fileName = totalParts[0].getOrigFileName();

        String tempFolder = System.getProperty("user.dir") + File.separator + "tempFile" + File.separator + md5;
        response.setHeader("content-type", "application/octet-stream");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

        // 创建文件夹
        Files.newFolder(tempFolder);
        List<String> chunkPath = new ArrayList<>();
        try {
            for (int i = 0; i < totalParts.length; i++) {
                byte[] fileByte = fastDFSUtils.downloadFile(totalParts[i].getGroupName(), totalParts[i].getFileId());
                Files.saveFile(fileByte, tempFolder, i + "-" + fileName);
                chunkPath.add(tempFolder + File.separator + i + "-" + fileName);
            }

            // 合并分片, 将文件保存到本地
            Files.merge(tempFolder + File.separator + fileName, chunkPath);

            // 下载本地文件
            FileInputStream fis = new FileInputStream(new File(tempFolder + File.separator + fileName));
            IOUtils.copy(fis, response.getOutputStream());
        } catch (Exception e) {
            log.error("文件合并失败", e);
            log.info("删除文件夹{}", tempFolder);
            Files.deleteDir(tempFolder);
            throw new BlegException(500, "文件合并失败", e);
        }
        // redisUtils.delete(redisKey + "*");
        log.info("删除文件夹{}", tempFolder);
        Files.deleteDir(tempFolder);
        long end = System.currentTimeMillis();
        log.info("time: {}", end - start);
    }

}

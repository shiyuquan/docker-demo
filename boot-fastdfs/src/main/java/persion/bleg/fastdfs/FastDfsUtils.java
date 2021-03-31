package persion.bleg.fastdfs;

import lombok.extern.slf4j.Slf4j;
import org.csource.fastdfs.*;
import org.springframework.stereotype.Component;
import persion.bleg.boot.base.BlegException;

import java.util.Properties;

/**
 * @author shiyuquan
 * @since 2020/5/9 10:39 上午
 */
@Slf4j
@Component
public class FastDfsUtils {

    public FastDfsUtils(FdfsProperty fdfsProperty) {
        Properties properties = new Properties();
        properties.setProperty("fastdfs.tracker_servers", fdfsProperty.getTrackerServers());
        properties.setProperty("fastdfs.connect_timeout_in_seconds", fdfsProperty.getConnectTimeoutInSeconds());
        properties.setProperty("fastdfs.network_timeout_in_seconds", fdfsProperty.getNetworkTimeoutInSeconds());
        properties.setProperty("fastdfs.charset", fdfsProperty.getCharset());
        properties.setProperty("fastdfs.http_anti_steal_token", fdfsProperty.getHttpAntiStealToken());
        properties.setProperty("fastdfs.http_secret_key", fdfsProperty.getHttpSecretKey());
        properties.setProperty("fastdfs.http_tracker_http_port", fdfsProperty.getHttpTrackerHttpPort());
        ConnectionPool connectionPool = fdfsProperty.getConnectionPool();
        properties.setProperty("fastdfs.connection_pool.enabled", connectionPool.getEnabled());
        properties.setProperty("fastdfs.connection_pool.max_count_per_entry", connectionPool.getMaxCountPerEntry());
        properties.setProperty("fastdfs.connection_pool.max_idle_time", connectionPool.getMaxIdleTime());
        properties.setProperty("fastdfs.connection_pool.max_wait_time_in_ms", connectionPool.getMaxWaitTimeInMs());
        try {
            ClientGlobal.initByProperties(properties);
            log.info("初始化 fdfs 配置: {}", ClientGlobal.configInfo());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    // private static final String CONFIG_FILENAME = "fdfs_client.conf";
    //
    // // 加载文件
    // static {
    //
    //     try {
    //         ClientGlobal.init(CONFIG_FILENAME);
    //         log.info("初始化 Fastdfs Client 配置信息：{}", ClientGlobal.configInfo());
    //
    //     } catch (Exception e) {
    //         log.error(e.toString(), e);
    //     }
    // }

    /**
     * 上传文件
     * @param fileContent
     * @param extName
     * @return
     * @throws Exception
     */
    public String[] uploadFile(byte[] fileContent, String extName) {
        StorageClient storageClient = getStorageClient(null);
        try {
            return storageClient.upload_file(fileContent, extName, null);
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
    public byte[] downloadFile(String groupName, String fileId) {
        StorageClient storageClient = getStorageClient(groupName);
        try {
            byte[] fileByte = storageClient.download_file(groupName, fileId);
            return fileByte;
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    public byte[] downloadFile(String uri) {
        String[] attr = splitUri(uri);
        StorageClient storageClient = getStorageClient(attr[0]);
        try {
            return storageClient.download_file(attr[0], attr[1]);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    /**
     * 删除文件
     * @param groupName
     * @param remoteFilename
     * @return
     */
    public int deleteFile(String groupName, String remoteFilename) {
        StorageClient storageClient = getStorageClient(groupName);
        try {
            return storageClient.delete_file(groupName, remoteFilename);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return 0;
        }

    }

    public int deleteFile(String uri) {
        String[] attr = splitUri(uri);
        StorageClient storageClient = getStorageClient(attr[0]);
        try {
            return storageClient.delete_file(attr[0], attr[1]);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return 0;
        }
    }

    public StorageClient getStorageClient(String groupName) {
        TrackerClient trackerClient;
        TrackerServer trackerServer;
        StorageServer storageServer;
        StorageClient storageClient;
        try {
            trackerClient = new TrackerClient();
            trackerServer = trackerClient.getTrackerServer();
            storageServer = trackerClient.getStoreStorage(trackerServer, groupName);
            storageClient = new StorageClient(trackerServer, storageServer);
        } catch (Exception e) {
            throw new BlegException("获取 StorageClient 失败 ");
        }
        return storageClient;
    }

    public static String[] splitUri(String uri) {
        int index = uri.indexOf('/');
        String[] attr = new String[2];
        if (-1 == index) {
            return attr;
        }
        attr[0] = uri.substring(0, index);
        attr[1] = uri.substring(index + 1);
        return attr;
    }
}

package persion.bleg.dockerdemo.minio;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * @author shiyuquan
 * @since 2020/5/7 9:50 上午
 */
@Slf4j
@Component
public class MinioClintService implements InitializingBean {

    private int lastMinioClientIndex = 0  ;

    private Map<Integer, MinioClient> minioClients = Maps.newHashMap() ;

    private Map<Integer,String> minioClientNames = Maps.newHashMap() ;

    private MinioConfig minioConfig;

    @Autowired
    public void setMinioConfig(MinioConfig minioConfig) {
        this.minioConfig = minioConfig;
    }

    /**
     * 选择上传客户端
     */
    public MinioClient selectClient(){
        int maxMinioClientIndex = minioClients.size() - 1 ;

        MinioClient minioClient = null ;

        String targetHost = null ;

        // 获取最新的 minioClient
        while ( minioClient == null ){
            minioClient = minioClients.get( lastMinioClientIndex ) ;
            targetHost = minioClientNames.get( lastMinioClientIndex ) ;
            lastMinioClientIndex ++ ;
            if( lastMinioClientIndex > maxMinioClientIndex ){
                lastMinioClientIndex = 0 ;
            }
        }

        log.info("当前选中的上传服务器:{}" , targetHost) ;

        return minioClient ;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText( minioConfig.getUrl() , "必须要配置minio的节点，不同的节点使用逗号分隔" );
        // 分隔节点信息
        List<String> endPoints = Splitter.on(",")
                .omitEmptyStrings()
                .trimResults()
                .splitToList( minioConfig.getUrl() ) ;

        int endPointIndex = 0 ;

        for (String endPoint : endPoints) {
            MinioClient client = null ;
            try {
                client = new MinioClient( endPoint , minioConfig.getAccessKey(), minioConfig.getSecretKey());
            } catch (Exception e) {
                log.info("初始化 minio client 异常, {}" , endPoint) ;
            }
            if( client == null ){
                continue;
            }

            // 保存 MinioClient
            minioClients.put( endPointIndex , client ) ;

            minioClientNames.put( endPointIndex , endPoint ) ;

            endPointIndex ++ ;
        }
    }
}

package persion.bleg.dockerdemo.fdfs;

import lombok.Data;
import lombok.ToString;

/**
 * @author shiyuquan
 * @since 2020/7/22 5:16 下午
 */
@Data
@ToString
public class ConnectionPool {
    private String enabled;
    private String maxCountPerEntry;
    private String maxIdleTime;
    private String maxWaitTimeInMs;
}

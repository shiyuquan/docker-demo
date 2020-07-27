package persion.bleg.dockerdemo.minio.my;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shiyuquan
 * @since 2020/4/22 10:24 上午
 */
public enum MyBucketRegionCache {
    INSTANCE;
    private final Map<String, String> regionMap = new ConcurrentHashMap<>();

    /** Returns AWS region for given bucket name. */
    public String region(String bucketName) {
        if (bucketName == null) {
            return "us-east-1";
        }

        String region = this.regionMap.get(bucketName);
        if (region == null) {
            return "us-east-1";
        } else {
            return region;
        }
    }

    /** Sets bucket name and its region to BucketRegionCache. */
    public void set(String bucketName, String region) {
        this.regionMap.put(bucketName, region);
    }

    /** Removes region cache of the bucket if any. */
    public void remove(String bucketName) {
        if (bucketName != null) {
            this.regionMap.remove(bucketName);
        }
    }

    /** Returns true if given bucket name is in the map else false. */
    public boolean exists(String bucketName) {
        return this.regionMap.get(bucketName) != null;
    }
}

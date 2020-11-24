package persion.bleg.minio.my;

import java.util.HashMap;
import java.util.Map;

/**
 * @author shiyuquan
 * @since 2020/4/22 10:12 上午
 */
public enum MyAwsS3Endpoints {
    INSTANCE;
    private final Map<String, String> endpoints = new HashMap<>();

    MyAwsS3Endpoints() {
        // ap-northeast-1
        endpoints.put("ap-northeast-1", "s3-ap-northeast-1.amazonaws.com");
        // ap-northeast-2
        endpoints.put("ap-northeast-2", "s3-ap-northeast-2.amazonaws.com");
        // ap-south-1
        endpoints.put("ap-south-1", "s3-ap-south-1.amazonaws.com");
        // ap-southeast-1
        endpoints.put("ap-southeast-1", "s3-ap-southeast-1.amazonaws.com");
        // ap-southeast-2
        endpoints.put("ap-southeast-2", "s3-ap-southeast-2.amazonaws.com");
        // eu-central-1
        endpoints.put("eu-central-1", "s3-eu-central-1.amazonaws.com");
        // eu-west-1
        endpoints.put("eu-west-1", "s3-eu-west-1.amazonaws.com");
        // eu-west-2
        endpoints.put("eu-west-2", "s3-eu-west-2.amazonaws.com");
        // sa-east-1
        endpoints.put("sa-east-1", "s3-sa-east-1.amazonaws.com");
        // us-west-1
        endpoints.put("us-west-1", "s3-us-west-1.amazonaws.com");
        // us-west-2
        endpoints.put("us-west-2", "s3-us-west-2.amazonaws.com");
        // us-east-1
        endpoints.put("us-east-1", "s3.amazonaws.com");
        // us-east-2
        endpoints.put("us-east-2", "s3-us-east-2.amazonaws.com");
        // ca-central-1
        endpoints.put("ca-central-1", "s3.ca-central-1.amazonaws.com");
        // cn-north-1
        endpoints.put("cn-north-1", "s3.cn-north-1.amazonaws.com.cn");
        // cn-northwest-1
        endpoints.put("cn-northwest-1", "s3.cn-northwest-1.amazonaws.com.cn");
    }

    /** Gets Amazon S3 endpoint for the relevant region. */
    public String endpoint(String region) {
        String s = MyAwsS3Endpoints.INSTANCE.endpoints.get(region);
        if (s == null) {
            s = "s3.amazonaws.com";
        }
        return s;
    }
}

package persion.bleg.business.constants;

/**
 * @author shiyuquan
 * @since 2020/3/19 2:38 下午
 */
public class DefalutConstant {

    private DefalutConstant() {}

    /** 默认请求成功的返回值 */
    public static final Integer RESPONSE_SUCCESS_CODE = 0;

    /** 默认请求失败的返回值 */
    public static final Integer RESPONSE_FAILED_CODE = -1;

    /** 传输体是否加密的标识，设置于http header */
    public static final String ENCRYPT_BODY_FLAG = "encryption";

    /** 后台配置文件内接口加密的配置名称 */
    public static final String ENCRYPT_CONFIG_NAME = "encrypt";

    /** 后台配置文件内防止重复请求的配置名称 */
    public static final String MALICIOUS_REQUEST_CONFIG_NAME = "malicious-request";

    /** minio配置 */
    public static final String MINIO_CONFIG_NAME = "minio";

    /** 最上层的接口路径前缀 */
    public static final String API = "/api";

    /** 接口版本 */
    public static final String API_VERSION1 = "/v1";

    /** 默认接口路径前缀 */
    public static final String DEFAULT_API_PREFIX = API + API_VERSION1;

    public static final String FAST_PROPERTY = "fastdfs";

    /** MQTT */
    public static final String MQTT = "mqtt";

}

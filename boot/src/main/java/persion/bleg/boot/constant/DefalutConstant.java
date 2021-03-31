package persion.bleg.boot.constant;

/**
 * @author shiyuquan
 * @since 2020/3/19 2:38 下午
 */
public class DefalutConstant {

    private DefalutConstant() {}

    /** 默认请求成功的返回值 */
    public static final String RESPONSE_SUCCESS_CODE = "0";

    /** 默认请求失败的返回值 */
    public static final String RESPONSE_FAILED_CODE = "-1";

    /** 最上层的接口路径前缀 */
    public static final String API = "/api";

    /** 接口版本 */
    public static final String API_VERSION1 = "/v1";

    /** 默认接口路径前缀 */
    public static final String DEFAULT_API_PREFIX = API + API_VERSION1;
}

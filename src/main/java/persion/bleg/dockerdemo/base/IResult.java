package persion.bleg.dockerdemo.base;

/**
 * 自定义返回体
 *
 * @author shiyuquan
 * @since 2020/3/19 1:48 下午
 */
public interface IResult<T> {

    /** 获取返回码 */
    Integer getCode();

    /** 获取返回消息 */
    String getMsg();

    /** 获取返回数据 */
    T getData();

    String getException();

    default String string() {
        return "{ \n" +
                "\tcode: " + getCode() + ",\n" +
                "\tmsg: " + getMsg() + ",\n" +
                "\tdata: " + getData() + "\n" +
                "}";
    }
}

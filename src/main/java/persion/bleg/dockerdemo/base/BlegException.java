package persion.bleg.dockerdemo.base;

/**
 * 自定义异常类
 *
 * @author shiyuquan
 * @since 2020/3/19 4:53 下午
 */
public class BlegException extends RuntimeException implements IResult<Object> {

    /** 错误码 */
    private Integer code;

    /** 错误消息 */
    private String msg;

    /** 错误内容 */
    private Object data;

    public BlegException() {
        super();
    }

    public BlegException(String msg) {
        this(500, msg, "");
    }


    public BlegException(Integer code, String msg) {
        this(code, msg, "");
    }


    public BlegException(Integer code, String msg, Object data) {
        super(msg);
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public BlegException(IMessage kc) {
        this(kc.getCode(), kc.getMsg(), "");
    }

    public BlegException(IMessage kc, Object data) {
        this(kc.getCode(), kc.getMsg(), data);
    }

    public BlegException(IResult ir) {
        this(ir.getCode(), ir.getMsg(), ir.getData());
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

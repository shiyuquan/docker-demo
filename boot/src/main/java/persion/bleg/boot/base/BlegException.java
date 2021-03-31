package persion.bleg.boot.base;

import persion.bleg.boot.utils.MessageUtils;

/**
 * 自定义异常类
 *
 * @author shiyuquan
 * @since 2020/3/19 4:53 下午
 */
public class BlegException extends RuntimeException {

    /** 错误码 */
    private String code;

    /** 错误消息 */
    private String msg;

    /** 错误内容 */
    private Object data;

    public BlegException() {
        super();
    }

    public BlegException(String msg) {
        this("500", msg, "");
    }


    public BlegException(String code, String msg) {
        this(code, msg, "");
    }

    public BlegException(String msg, Object data) {
        this("500", msg, data);
    }

    public BlegException(String code, String msg, Object data) {
        super(msg);
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public BlegException(IMessage kc) {
        this(kc.getCode(), MessageUtils.get(kc.getMsg()), "");
    }

    public BlegException(IMessage kc, Object data) {
        this(kc.getCode(), MessageUtils.get(kc.getMsg()), data);
    }

    public BlegException(IResult ir) {
        this(ir.getCode(), ir.getMsg(), ir.getData());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

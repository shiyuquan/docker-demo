package persion.bleg.dockerdemo.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import persion.bleg.dockerdemo.constants.MessageKeycode;
import persion.bleg.dockerdemo.util.MessageUtils;

import static persion.bleg.dockerdemo.constants.DefalutConstant.RESPONSE_FAILED_CODE;
import static persion.bleg.dockerdemo.constants.DefalutConstant.RESPONSE_SUCCESS_CODE;
import static persion.bleg.dockerdemo.constants.MessageKeycode.FAILED;
import static persion.bleg.dockerdemo.constants.MessageKeycode.SUCCESS;

/**
 * 自定义返回类型
 *
 * @author shiyuquan
 * @since 2020/3/19 1:56 下午
 */
public class Result<T> implements IResult<T> {

    /** 返回码 */
    private Integer code;

    /** 返回消息 */
    private String msg;

    /** 返回内容 */
    private T data;

    public Result() {}

    public Result(Integer code) {
        this.code = code;
        this.msg = "";
        this.data = null;
    }

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
        this.data = null;
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Result<T> success() {
        this.code = SUCCESS.getCode();
        this.msg = MessageUtils.get(SUCCESS.getMsg());
        this.data = null;
        return this;
    }

    public Result<T> success(T data) {
        this.code = SUCCESS.getCode();
        this.msg = MessageUtils.get(SUCCESS.getMsg());
        this.data = data;
        return this;
    }

    public Result<T> success(MessageKeycode kc, T data) {
        this.code = kc.getCode();
        this.msg = MessageUtils.get(kc.getMsg());
        this.data = data;
        return this;
    }

    public Result<T> err(String msg) {
        this.code = FAILED.getCode();
        this.msg = msg;
        this.data = null;
        return this;
    }

    public Result<T> err(Integer code) {
        this.code = code;
        this.msg = MessageUtils.get(FAILED.getMsg());
        this.data = null;
        return this;
    }

    public Result<T> err(MessageKeycode kc) {
        this.code = kc.getCode();
        this.msg = MessageUtils.get(kc.getMsg());
        this.data = null;
        return this;
    }

    /**
     * 抛出异常
     */
    public void te() {
        throw new BlegException(this);
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public T getData() {
        return data;
    }
}

package persion.bleg.boot.base;

import persion.bleg.boot.utils.MessageUtils;

import static persion.bleg.boot.constant.MessageKeycode.FAILED;
import static persion.bleg.boot.constant.MessageKeycode.SUCCESS;

/**
 * 自定义返回类型
 *
 * @author shiyuquan
 * @since 2020/3/19 1:56 下午
 */
public class Result<T> implements IResult<T> {

    /** 返回码 */
    private String code;

    /** 返回消息 */
    private String msg;

    /** 返回内容 */
    private T data;

    private String exception;

    public Result() {}

    public Result(String code) {
        this.code = code;
        this.msg = "";
        this.data = null;
    }

    public Result(String code, String msg) {
        this.code = code;
        this.msg = msg;
        this.data = null;
    }

    public Result(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Result(String code, String msg, T data, String exception) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.exception = exception;
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

    public Result<T> success(IMessage kc, T data) {
        this.code = kc.getCode();
        this.msg = MessageUtils.get(kc.getMsg());
        this.data = data;
        return this;
    }

    public Result<T> err() {
        this.code = FAILED.getCode();
        this.msg = MessageUtils.get(FAILED.getMsg());
        this.data = null;
        return this;
    }

    public Result<T> err(String msg) {
        this.code = FAILED.getCode();
        this.msg = msg;
        this.data = null;
        return this;
    }

    public Result<T> err(String code, String msg) {
        this.code = code;
        this.msg = msg;
        this.data = null;
        return this;
    }

    public Result<T> err(IMessage kc) {
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

    public void setCode(String code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    @Override
    public String getCode() {
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

    @Override
    public String getException() {
        return exception;
    }
}

package persion.bleg.boot.constant;

import persion.bleg.boot.base.IMessage;

import static persion.bleg.boot.constant.DefalutConstant.RESPONSE_FAILED_CODE;
import static persion.bleg.boot.constant.DefalutConstant.RESPONSE_SUCCESS_CODE;

/**
 * 国际化消息枚举类
 *
 * @author shiyuquan
 * @since 2020/3/19 4:15 下午
 */
public enum MessageKeycode implements IMessage {

    SUCCESS(RESPONSE_SUCCESS_CODE, "success"),
    FAILED(RESPONSE_SUCCESS_CODE, "failed"),
    SERVER_ERROR(RESPONSE_FAILED_CODE, "SERVER_ERROR"),

    ;

    String code;

    String msg;

    MessageKeycode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

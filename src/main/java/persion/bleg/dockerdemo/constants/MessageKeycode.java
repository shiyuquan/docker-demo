package persion.bleg.dockerdemo.constants;

import static persion.bleg.dockerdemo.constants.DefalutConstant.RESPONSE_FAILED_CODE;
import static persion.bleg.dockerdemo.constants.DefalutConstant.RESPONSE_SUCCESS_CODE;

/**
 * 国际化消息枚举类
 *
 * @author shiyuquan
 * @since 2020/3/19 4:15 下午
 */
public enum MessageKeycode {

    SUCCESS(RESPONSE_SUCCESS_CODE, "success"),
    FAILED(RESPONSE_SUCCESS_CODE, "failed"),
    SERVER_ERROR(RESPONSE_FAILED_CODE, "SERVER_ERROR"),

    ;

    Integer code;

    String msg;

    MessageKeycode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

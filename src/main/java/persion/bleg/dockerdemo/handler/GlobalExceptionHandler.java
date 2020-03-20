package persion.bleg.dockerdemo.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import persion.bleg.dockerdemo.base.BlegException;
import persion.bleg.dockerdemo.base.IResult;
import persion.bleg.dockerdemo.base.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static persion.bleg.dockerdemo.constants.MessageKeycode.SERVER_ERROR;

/**
 * 全局异常处理类
 *
 * @author shiyuquan
 * @since 2020/3/20 4:53 下午
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    public IResult<Object> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        Integer code = SERVER_ERROR.getCode();
        String msg = SERVER_ERROR.getMsg();
        Object data = "";
        log.error("GlobalExceptionHandler: uri: {}, method: {}", request.getRequestURI(), request.getMethod());
        if (e instanceof BlegException) {
            BlegException blegException = (BlegException) e;
            code = blegException.getCode();
            msg = blegException.getMsg();
            data = blegException.getData();
            log.error("GlobalExceptionHandler: code: {}, msg: {}", code, msg);
            log.error("GlobalExceptionHandler: ", e);
        }
        response.setStatus(500);
        return new Result<Object>(code, msg, data);
    }

}

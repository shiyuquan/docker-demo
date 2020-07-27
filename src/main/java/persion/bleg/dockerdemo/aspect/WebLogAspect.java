package persion.bleg.dockerdemo.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 所有controller的切面
 *
 * @author shiyuquan
 * @since 2020/3/25 9:51 上午
 */
@Component
@Aspect
@Slf4j
public class WebLogAspect {

    private HttpServletRequest request;

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Pointcut("execution(public * persion.bleg..*Controller.*(..))")
    public void webLog() {}

    @Before("webLog()")
    public void doBefor(JoinPoint joinPoint) {
        // 获取uri
        String uri = request.getRequestURI();
        // 获取请求方法
        String method = request.getMethod();
        // 获取类和方法名
        String classMethodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
        // 获取请求参数
        String args = getArgs(joinPoint);
        log.info("WebLogAspect: uri: {}, method: {}", uri, method);
        log.info("WebLogAspect: classMethod: {}, args: {}", classMethodName, args);
    }

    @AfterReturning(pointcut = "webLog()")
    public void doAfterReturning() {

    }

    /**
     * 获取切点的请求参数
     *
     * @param joinPoint 切点
     * @return 参数
     */
    public String getArgs(JoinPoint joinPoint) {
        Object[] arr = joinPoint.getArgs();
        List<String> list = new ArrayList<>();
        for (Object o : arr) {
            if (o == null) {
                continue;
            }
            if (o instanceof HttpServletRequest || o instanceof HttpServletResponse) {
                continue;
            }
            list.add(o.toString());
        }
        return list.toString();
    }
}

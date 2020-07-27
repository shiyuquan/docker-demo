package persion.bleg.dockerdemo.maliciousrequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import persion.bleg.dockerdemo.base.BlegException;
import persion.bleg.dockerdemo.util.RedisUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 防恶意请求的过滤器
 *
 * @author shiyuquan
 * @since 2020/3/23 5:30 下午
 */
@Slf4j
@Component
public class MaliciousRequestFilter implements Filter {

    private static final String PRE = "maliciousRequest:";
    private static final String REQUEST_LOG = "log:";
    private static final String LOCK_IP = "lockIp:";

    private MaliciousRequestConfig maliciousRequestConfig;
    private RedisUtils redisUtils;

    @Autowired
    public void setRedisUtils(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    @Autowired
    public void setMaliciousRequestConfig(MaliciousRequestConfig maliciousRequestConfig) {
        this.maliciousRequestConfig = maliciousRequestConfig;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // 判断是否需要进行拦截
        Boolean b = maliciousRequestConfig.getOpen();
        if (Boolean.TRUE.equals(b)) {

            // 获取ip
            String ip = request.getRemoteHost();
            // 获取uri
            String uri = request.getRequestURI();
            // 获取请求方法
            String method = request.getMethod();

            // 判断ip是否被锁
            String lockIpKey = PRE + LOCK_IP + ip;
            Long lockTime = (Long) redisUtils.get(lockIpKey);
            // redis 查询key没有结果会返回null，如果这里lockTime为null说明没被锁
            if (null != lockTime) {
                log.error("请求过于频繁: {}", lockIpKey);
                long sub = System.currentTimeMillis() - lockTime;
                throw new BlegException(500, "请求过于频繁，" + sub + "ms后重新尝试");
            }

            // 定义redis key
            String saveKey = PRE + REQUEST_LOG + ip + ":" + method + " " + uri;

            // 获取符合标准的key的个数
            int total = redisUtils.getKeyNum(saveKey + "*");
            // 如果缓存的key大于设置的安全请求个数
            if (total >= maliciousRequestConfig.getSafetyNum()) {
                // 设置上锁的ip于redis，并设置过期时间
                redisUtils.setWithTtl(lockIpKey, System.currentTimeMillis(), maliciousRequestConfig.getLimitedTime());

                log.error("请求过于频繁: {}", saveKey);
                throw new BlegException(500, "请求过于频繁");
            }
            // 将每个请求的详细内容作为key，存放于redis，设置过期时间
            String realKey = saveKey + System.currentTimeMillis();
            redisUtils.setWithTtl(realKey, realKey, maliciousRequestConfig.getTimeInterval());

        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}

package persion.bleg.dockerdemo.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static persion.bleg.dockerdemo.constants.DefalutConstant.ENCRYPT_BODY_FLAG;

/**
 * 跨域请求过滤处理类
 *
 * @author shiyuquan
 * @since 2020/3/23 8:54 上午
 */
public class CorsRequestFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        String requestMethod = request.getMethod();
        if ("OPTIONS".equals(requestMethod.toUpperCase())) {
            Object origin = request.getHeader("origin");
            Object connection = request.getHeader("connection");
            Object accessRequestMthod = request.getHeader("Access-Control-Request-Method");
            Object accessRequestHeaders = request.getHeader("Access-Control-Request-Headers");

            // HTTP 204 No Content 成功状态响应码，表示该请求已经成功了，但是客户端客户不需要离开当前页面
            response.setStatus(204);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", origin == null ? "*": origin.toString());
            response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, " + ENCRYPT_BODY_FLAG);
            response.setHeader("Access-Control-Allow-Methods", accessRequestMthod == null ? "POST, PUT, GET, OPTIONS, DELETE, HEAD": accessRequestMthod.toString());
            // 支持HTTP 1.1.
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Connection", connection == null ? "keep-alive": connection.toString());
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}

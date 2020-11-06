package persion.bleg.dockerdemo.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author shiyuquan
 * @since 2020/11/4 4:34 下午
 */
@Component
@Order(0)
public class Log4J2StartedEventListener implements GenericApplicationListener {

    // public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    private static Class<?>[] EVENT_TYPES = {ApplicationStartingEvent.class,
                                             ApplicationEnvironmentPreparedEvent.class, ApplicationPreparedEvent.class,
                                             ContextClosedEvent.class, ApplicationFailedEvent.class };

    private static Class<?>[] SOURCE_TYPES = { SpringApplication.class,
                                               ApplicationContext.class };
    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private String port;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
            System.err.println("--- chushihua ");
            MDC.put("appName", appName);
            MDC.put("port", port);
    }

    @Override
    public boolean supportsEventType(ResolvableType resolvableType) {
        return isAssignableFrom(resolvableType.getRawClass(), EVENT_TYPES);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return isAssignableFrom(sourceType, SOURCE_TYPES);
    }

    private boolean isAssignableFrom(Class<?> type, Class<?>... supportedTypes) {
        if (type != null) {
            for (Class<?> supportedType : supportedTypes) {
                if (supportedType.isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}

package persion.bleg.boot.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 国际化消息工具类
 *
 * @author shiyuquan
 * @since 2020/3/19 2:10 下午
 */
public class MessageUtils {
    private MessageUtils() {}

    /**
     * 获取单个国际化翻译值
     */
    public static String get(String msg) {
        MessageSource m = SpringContextUtils.getBean(MessageSource.class);
        return m.getMessage(msg, null, LocaleContextHolder.getLocale());
    }

    /**
     * 获取单个国际化翻译值
     */
    public static String get(String msg, Object... args) {
        MessageSource m = SpringContextUtils.getBean(MessageSource.class);
        return m.getMessage(msg, args, LocaleContextHolder.getLocale());
    }

}

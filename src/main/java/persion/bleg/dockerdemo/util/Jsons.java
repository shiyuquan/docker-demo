package persion.bleg.dockerdemo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import persion.bleg.dockerdemo.base.BlegException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * json工具 使用 ObjectMapper
 *
 * @author shiyuquan
 * @since 2020/4/28 11:38 上午
 */
@Slf4j
public class Jsons {

    private Jsons() {}

    /** 获取单利模式的 ObjectMapper */
    public static ObjectMapper getOm() {
        return Singleton.instence.om;
    }

    private enum Singleton {
        /** 实例 枚举 */
        instence;

        private ObjectMapper om;

        /**
         * JVM 保证此方法只调用一次
         */
        Singleton () {
            this.om = new ObjectMapper();
            // 忽略不认识的字段的检查
            om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // 转换为格式化的js
            om.enable(SerializationFeature.INDENT_OUTPUT);
        }

        public void setOm(ObjectMapper om) {
            this.om = om;
        }
    }

    /**
     * 判读字符串是不是json
     * @param str 字符串
     * @return boolean
     */
    public static boolean isJson(String str) {
        try {
            getOm().readTree(str);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * json 转 map
     * @param json json
     * @return map
     */
    public static Map jsonToMap(String json) {
        if (!isJson(json)) {
            throw new BlegException(500, "该字符串不是json");
        }
        Map map = new HashMap(16);
        try {
            map = getOm().readValue(json, Map.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new BlegException(500, "json 转 map 失败");
        }
        return map;
    }

    /**
     * object 转 对象
     * @param source 来源
     * @param clazz 目标
     * @param <T> 类型
     * @return 对象
     */
    public static <T> T convertValue(Object source, Class<T> clazz) {
        T data = null;
        try {
            data = getOm().convertValue(source, clazz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BlegException(500, "对象转化失败，请检查对象属性");
        }
        return data;
    }

    /**
     *  obj 转 byte数组
     * @param o obj
     * @return 字节数组
     */
    public static byte[] toBytes(Object o) {
        try {
            return getOm().writeValueAsBytes(o);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * obj 转 json
     * @param o 对象
     * @return json
     */
    public static String toJson(Object o) {
        try {
            return getOm().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * json 转 对象
     * @param json 来源
     * @param tClass 目标
     * @param <T> 类型
     * @return 对象
     */
    public static <T> T parseObject(String json, Class<T> tClass) {
        try {
            return getOm().readValue(json, tClass);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static String[] getValuePropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (null != srcValue) {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public static void main(String[] args) throws Exception {

        String d = "2020-01-01";
        String e = "2020-08-08";

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        Date t1 = format.parse(d);
        Date t2 = format.parse(e);

        System.err.println(t1.before(t2));
    }
}

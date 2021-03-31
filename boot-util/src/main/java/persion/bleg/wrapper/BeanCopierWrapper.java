package persion.bleg.wrapper;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高性能的 bean 转换类
 *
 * 很多系统里面 从页面请求体的访问到结果的数据，经历了 BO -> DTO -> DO -> DTO -> VO 等的多层bean 转换。数据多，访问大将会
 * 造成性能上的问题。所以会有这个工具类，专门用于 bean 的转换，提高功能效率。
 *
 * cglib -> BeanCopier。 通过修改字节码，生成类似于 B.setXXX(A.getXXX()) 的代码。因为是通过字节码来 get/set 方式，
 * 所以效率只比手动代码去 get/set 慢一点点，比 BeanUtils 等反射工具快数十倍。
 *
 * ASM -> ReflectASM. 使用字节码生成的方式实现了更为高效的反射机制。执行时会生成一个存取类来 set/get 字段，访问方法或创建实例。
 * 用字节码生成的方式代替 Java 本身的反射机制来实现它更快，并且避免了访问原始类型因自动装箱而产生的问题。
 *
 * @author shiyuquan
 * @since 2021/3/31 3:55 下午
 */
@Slf4j
public class BeanCopierWrapper {

    /** 缓存的 BeanCopier */
    private static final Map<String, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>();
    /** 缓存的 ConstructorAccess */
    private static final Map<String, ConstructorAccess> CONSTRUCTOR_ACCESS_CACHE = new ConcurrentHashMap<>();

    /**
     * 复制参数
     *
     * @param source 原对象
     * @param target 目标对象
     */
    private static void copyProperties(Object source, Object target) {
        BeanCopier copier = getBeanCopier(source.getClass(), target.getClass());
        copier.copy(source, target, null);
    }

    /**
     * 获取 BeanCopier 第一次缓存，提高性能
     *
     * @param sourceClass 原对象
     * @param targetClass 目标对象
     * @return BeanCopier
     */
    private static BeanCopier getBeanCopier(Class sourceClass, Class targetClass) {
        String beanKey = generateKey(sourceClass, targetClass);
        BeanCopier copier = null;
        if (!BEAN_COPIER_CACHE.containsKey(beanKey)) {
            copier = BeanCopier.create(sourceClass, targetClass, false);
            BEAN_COPIER_CACHE.put(beanKey, copier);
        } else {
            copier = BEAN_COPIER_CACHE.get(beanKey);
        }
        return copier;
    }

    /**
     * 生成缓存的key
     *
     * @param class1 原对象
     * @param class2 目标对象
     * @return 缓存的key
     */
    private static String generateKey(Class<?> class1, Class<?> class2) {
        return class1.toString() + class2.toString();
    }

    /**
     * 复制参数
     *
     * @param source 原对象
     * @param targetClass 目标对象
     * @param <T> 目标对象类型
     * @return 目标对象实例
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        T t = null;
        try {
            t = targetClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Create new instance of {} failed: {}", targetClass, e.getMessage());
            throw new RuntimeException("Create new instance of " + targetClass + " failed: " + e.getMessage());
        }
        copyProperties(source, t);
        return t;
    }

    /**
     * list 的转换
     *
     * @param sourceList 源列表
     * @param targetClass 目标对象
     * @param <T> 目标对象类型
     * @return 目标对象列表
     */
    public static <T> List<T> copyPropertiesOfList(List<?> sourceList, Class<T> targetClass) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        ConstructorAccess<T> constructorAccess = getConstructorAccess(targetClass);
        List<T> resultList = new ArrayList<>(sourceList.size());
        for (Object o : sourceList) {
            T t = null;
            try {
                t = constructorAccess.newInstance();
            } catch (Exception e) {
                log.error("Create new instance of {} failed: {}", targetClass, e.getMessage());
                throw new RuntimeException("Create new instance of " + targetClass + " failed: " + e.getMessage());
            }
            copyProperties(o, t);
            resultList.add(t);
        }
        return resultList;
    }

    /**
     * 获取 反射 对象
     * @param targetClass 目标对象
     * @param <T> 目标对象类型
     * @return 反射的构造类
     */
    private static <T> ConstructorAccess<T> getConstructorAccess(Class<T> targetClass) {
        ConstructorAccess<T> constructorAccess = CONSTRUCTOR_ACCESS_CACHE.get(targetClass.toString());
        if (constructorAccess != null) {
            return constructorAccess;
        }
        try {
            constructorAccess = ConstructorAccess.get(targetClass);
            constructorAccess.newInstance();
            CONSTRUCTOR_ACCESS_CACHE.put(targetClass.toString(), constructorAccess);
        } catch (Exception e) {
            log.error("Create new instance of {} failed: {}", targetClass, e.getMessage());
            throw new RuntimeException("Create new instance of " + targetClass + " failed: " + e.getMessage());
        }
        return constructorAccess;
    }

}

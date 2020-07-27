package persion.bleg.dockerdemo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis 操作工具类，redisTemplate
 *
 * @author shiyuquan
 * @since 2020/3/24 8:23 下午
 */
@Slf4j
@Component
public class RedisUtils {

    private RedisUtils() {}

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 给定匹配模式的key，获取符合结果的个数
     *
     * @param pattern key
     * @return size
     */
    public int getKeyNum(String pattern) {
        Set<String> set = redisTemplate.keys(pattern);
        if (!CollectionUtils.isEmpty(set)) {
            return set.size();
        }
        return 0;
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key 键
     * @param value 值
     * @param time 时间(毫秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean setWithTtl(String key, Object value, Long time) {
        if (time > 0) {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
        return true;
    }

    /**
     * 普通缓存存放
     *
     * @param key 键
     * @param value 值
.     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定的key 匹配模式
     * @param keys key
     * @return 个数
     */
    public Long delete(String keys) {
        Set<String> keySet = redisTemplate.keys(keys);
        if (CollectionUtils.isEmpty(keySet)) {
            return 0L;
        }
        return redisTemplate.delete(keySet);
    }
}

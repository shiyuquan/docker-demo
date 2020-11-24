package persion.bleg.business.core.demo.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import persion.bleg.boot.base.IResult;
import persion.bleg.boot.base.Result;
import persion.bleg.business.core.demo.entity.User;
import persion.bleg.redis.RedisUtils;

import static persion.bleg.business.constants.DefalutConstant.DEFAULT_API_PREFIX;

/**
 * @author shiyuquan
 * @since 2020/3/24 8:40 下午
 */
@Api(tags = "redis测试接口")
@RestController
@RequestMapping(DEFAULT_API_PREFIX + "/redis")
public class RedisController {

    private RedisTemplate<String, Object> redisTemplate;
    private RedisUtils redisUtils;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setRedisUtils(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    /**
     * 新增对象，redisTemplate.opsForValue
     *
     * @param user 对象
     * @return 操作结果
     */
    @ApiOperation(value = "新增对象，redisTemplate.opsForValue")
    @PostMapping(value = "/object/opsForValue")
    public IResult<Boolean> addObject(@RequestBody User user) {
        redisTemplate.opsForValue().set("redis:object:user", user);
        return new Result<Boolean>().success(true);
    }

    /**
     * 获取 redisTemplate.opsForValue() 的对象
     *
     * @return 对象
     */
    @ApiOperation(value = "获取 redisTemplate.opsForValue() 的对象")
    @GetMapping(value = "/object/opsForValue")
    public IResult<User> getObject() {
        Object obj = redisTemplate.opsForValue().get("redis:object:user");
        User user = (User) obj;
        return new Result<User>().success(user);
    }

    /**
     * 移除 redisTemplate.opsForValue() 的对象
     *
     * @return 对象
     */
    @ApiOperation(value = "获取 redisTemplate.opsForValue() 的对象")
    @DeleteMapping(value = "/object/opsForValue")
    public IResult<Boolean> removeObject() {
        return new Result<Boolean>().success(redisTemplate.delete("redis:object:user"));
    }


    @ApiOperation(value = "新增对象，redisTemplate.opsForValue")
    @PostMapping(value = "/object/test")
    public IResult<Boolean> test() {
        redisTemplate.opsForValue().set("1.1.1.1:/api/v1/test1", System.currentTimeMillis());
        redisTemplate.opsForValue().set("1.1.1.1:/api/v1/test2", System.currentTimeMillis());
        redisTemplate.opsForValue().set("1.1.1.2:/api/v1/test1", System.currentTimeMillis());
        redisUtils.setWithTtl("1.1.1.2:/api/v1/test2", System.currentTimeMillis(), 5000L);
        return new Result<Boolean>().success(true);
    }

    /**
     * 获取 redisTemplate.opsForValue() 的对象
     *
     * @return 对象
     */
    @ApiOperation(value = "")
    @GetMapping(value = "/object/testGet")
    public IResult<Boolean> gbject() {
        Long l = (Long) redisUtils.get("longTest");
        Long l2 = (Long) redisTemplate.opsForValue().get("longTest");


        redisTemplate.opsForValue().set("boolean", true);
        redisTemplate.opsForValue().set("longTest", 123111341234L);
        Boolean b = (Boolean) redisTemplate.opsForValue().get("boolean");
        return new Result<Boolean>().success(b);
    }
}

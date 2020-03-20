package persion.bleg.dockerdemo.core.demo.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import persion.bleg.dockerdemo.base.BlegException;
import persion.bleg.dockerdemo.base.IResult;
import persion.bleg.dockerdemo.base.Result;
import persion.bleg.dockerdemo.core.demo.entity.User;
import persion.bleg.dockerdemo.core.demo.service.UserService;
import persion.bleg.dockerdemo.encryptbody.annotation.DecryptBody;

import java.util.List;

/**
 * @author shiyuquan
 * @since 2019/12/23 2:32 下午
 */
@Api(value = "测试用户接口")
@RestController
@RequestMapping()
public class UserController {

    private UserService userService;
    private RedisTemplate redisTemplate;
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @ApiOperation(value = "列表查询")
    @GetMapping(value = "/users")
    public List<User> selectUser() {
        return userService.selectUser();
    }

    @ApiOperation(value = "根据名称查询")
    @GetMapping(value = "/user/{name}")
    public User selectByName(@PathVariable("name") String name) {
        return userService.selectByName(name);
    }

    @DecryptBody
    @ApiOperation(value = "新增")
    @PostMapping(value = "/user")
    public Boolean add(@RequestBody User user) {
        return userService.add(user);
    }

    @ApiOperation(value = "redis新增user对象")
    @PostMapping(value = "/user-to-redis")
    public Boolean addToRedis(@RequestBody User user) {
        return userService.add(user);
    }

    @ApiOperation(value = "列表查询")
    @GetMapping(value = "/test")
    public IResult<User> test() {
        throw new BlegException(500, "ssdsd");
        // return new Result<User>().success();
    }
}

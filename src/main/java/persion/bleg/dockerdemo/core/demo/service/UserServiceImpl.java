package persion.bleg.dockerdemo.core.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import persion.bleg.dockerdemo.config.mp.IServiceImpl;
import persion.bleg.dockerdemo.core.demo.entity.User;
import persion.bleg.dockerdemo.core.demo.mapper.UserMapper;

import java.util.List;

/**
 * @author shiyuquan
 * @since 2019/12/23 2:25 下午
 */
@Slf4j
@Service
public class UserServiceImpl extends IServiceImpl<UserMapper, User> implements UserService {

    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询用户列表
     *
     * @return 用户数组
     */
    @Override
    public List<User> selectUser() {
        return baseMapper.selectUser();
    }

    /**
     * 根据名称查询用户
     *
     * @param name 用户名
     * @return 用户
     */
    @Override
    public User selectByName(String name) {
        return getOne(wrapper().eq("name", name));
    }

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 成功与否
     */
    @Override
    public boolean add(User user) {
        return save(user);
    }

}

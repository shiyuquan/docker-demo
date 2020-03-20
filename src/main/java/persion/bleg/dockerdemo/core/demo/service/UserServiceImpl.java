package persion.bleg.dockerdemo.core.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netflix.ribbon.proxy.annotation.Http;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Request;
import org.springframework.stereotype.Service;
import persion.bleg.dockerdemo.core.demo.entity.User;
import persion.bleg.dockerdemo.core.demo.mapper.UserMapper;

import javax.servlet.http.HttpServlet;
import java.util.List;

/**
 * @author shiyuquan
 * @since 2019/12/23 2:25 下午
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public List<User> selectUser() {
        return baseMapper.selectUser();
    }

    @Override
    public User selectByName(String name) {
        return getOne(new QueryWrapper<User>().eq("name", name));
    }

    @Override
    public boolean add(User user) {
        return save(user);
    }


}

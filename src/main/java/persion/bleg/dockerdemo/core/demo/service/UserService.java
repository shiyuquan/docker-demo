package persion.bleg.dockerdemo.core.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import persion.bleg.dockerdemo.core.demo.entity.User;

import java.util.List;

/**
 * @author shiyuquan
 * @since 2019/12/23 2:23 下午
 */
public interface UserService extends IService<User> {

    List<User> selectUser();

    User selectByName(String name);

    boolean add(User user);

}

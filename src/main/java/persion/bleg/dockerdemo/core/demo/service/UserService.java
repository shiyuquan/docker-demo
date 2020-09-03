package persion.bleg.dockerdemo.core.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import persion.bleg.dockerdemo.core.demo.entity.User;

import java.util.List;

/**
 * @author shiyuquan
 * @since 2019/12/23 2:23 下午
 */
public interface UserService extends IService<User> {

    /**
     * 查询用户列表
     *
     * @return 用户数组
     */
    List<User> selectUser();

    /**
     * 根据名称查询用户
     *
     * @param name 用户名
     * @return 用户
     */
    User selectByName(String name);

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 成功与否
     */
    boolean add(User user);

    /**
     * 用户上传图片
     *
     * @param id 用户id
     * @param file 用户上传的图片
     * @return 成功与否
     */
    Boolean addImage(String id, MultipartFile file);
}

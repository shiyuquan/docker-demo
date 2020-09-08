package persion.bleg.dockerdemo.core.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * 分页查询
     *
     * @param page 页数
     * @param size 页大小
     * @return {@link IPage} {@link User}
     */
    IPage<User> selectUserPage(Integer page, Integer size);

    /**
     * 根据名称查询用户
     *
     * @param name 用户名
     * @return 用户
     */
    User selectByName(String name);

    /**
     * 新增对象
     *
     * @param user user
     * @return 成功与否
     */
    boolean addUser(User user);

    /**
     * 用户上传图片
     *
     * @param id 用户id
     * @param file 用户上传的图片
     * @return 成功与否
     */
    Boolean addImage(String id, MultipartFile file);

    /**
     * 查询列表
     * @return {@link List} {@link User}
     */
    List<User> selectUserList();

    /**
     * 根据id查询
     * @param id 主键
     * @return {@link User}
     */
    User selectById(Integer id);

    /**
     * 修改对象
     * @param user user
     * @return {@link Boolean}
     */
    Boolean updateUserById(User user);

    /**
     * 根据id删除
     * @param id 主键
     * @return {@link Boolean}
     */
    Boolean deleteById(Integer id);
}

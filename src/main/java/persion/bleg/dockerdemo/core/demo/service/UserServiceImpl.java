package persion.bleg.dockerdemo.core.demo.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import persion.bleg.dockerdemo.config.mp.IServiceImpl;
import persion.bleg.dockerdemo.core.demo.entity.User;
import persion.bleg.dockerdemo.core.demo.mapper.UserMapper;

import java.io.IOException;
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
     * 分页查询
     *
     * @param page 页数
     * @param size 页大小
     * @return {@link IPage} {@link User}
     */
    @Override
    public IPage<User> selectUserPage(Integer page, Integer size) {
        Page<User> pageInfo = new Page<>(page, size, true);
        return baseMapper.selectPage(pageInfo, wrapper().orderByDesc(User.ID));
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
     * 新增对象
     *
     * @param user user
     * @return 成功与否
     */
    @Override
    public boolean addUser(User user) {
        return save(user);
    }

    /**
     * 用户上传图片
     *
     * @param id   用户id
     * @param file 用户上传的图片
     * @return 成功与否
     */
    @Override
    public Boolean addImage(String id, MultipartFile file) {
        try {
            return update(new UpdateWrapper<User>().eq("id", id).set("image", file.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 查询列表
     *
     * @return {@link List} {@link User}
     */
    @Override
    public List<User> selectUserList() {
        return baseMapper.selectList(null);
    }

    /**
     * 根据id查询
     *
     * @param id 主键
     * @return {@link User}
     */
    @Override
    public User selectById(Integer id) {
        return getOne(wrapper().eq(User.ID, id));
    }

    /**
     * 修改对象
     *
     * @param user user
     * @return {@link Boolean}
     */
    @Override
    public Boolean updateUserById(User user) {
        return updateById(user);
    }

    /**
     * 根据id删除
     *
     * @param id 主键
     * @return {@link Boolean}
     */
    @Override
    public Boolean deleteById(Integer id) {
        return removeById(id);
    }

}

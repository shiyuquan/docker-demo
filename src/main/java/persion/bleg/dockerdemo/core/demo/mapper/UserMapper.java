package persion.bleg.dockerdemo.core.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import persion.bleg.dockerdemo.core.demo.entity.User;

import java.util.List;

/**
 * @author shiyuquan
 * @since 2019/12/23 2:13 下午
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    List<User> selectUser();
}

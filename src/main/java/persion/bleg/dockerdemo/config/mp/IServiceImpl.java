package persion.bleg.dockerdemo.config.mp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @author shiyuquan
 * @since 2020/8/21 10:59 上午
 */
public class IServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {

    public QueryWrapper<T> wrapper() {
        return new QueryWrapper<>();
    }
}

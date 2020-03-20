package persion.bleg.dockerdemo.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.util.Date;

import static persion.bleg.dockerdemo.constants.DbConstant.*;

/**
 * @author shiyuquan
 * @since 2019/12/23 1:15 下午
 */
@Slf4j
@Component
public class BaseEntityMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        setFieldValByName(CREATE_TIME, now, metaObject);
        setFieldValByName(CREATOR, "admin", metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        setFieldValByName(MODIFY_TIME, now, metaObject);
        setFieldValByName(MODIFIER, "admin", metaObject);
    }

}

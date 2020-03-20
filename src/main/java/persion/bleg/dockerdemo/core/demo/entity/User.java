package persion.bleg.dockerdemo.core.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import persion.bleg.dockerdemo.base.BaseEntity;

import java.util.UUID;

/**
 * @author shiyuquan
 * @since 2019/12/23 11:19 上午
 */
@Getter
@Setter
@TableName("t_user")
@ApiModel(value = "User", description = "测试用户表")
public class User extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private Integer age;

    private String nickName;

}

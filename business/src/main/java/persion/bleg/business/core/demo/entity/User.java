package persion.bleg.business.core.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import persion.bleg.mybatis.mp.BaseEntity;

/**
 * @author shiyuquan
 * @since 2019/12/23 11:19 上午
 */
@Getter
@Setter
@TableName("t_user")
@ApiModel(value = "User", description = "测试用户表")
@ToString
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private long id;

    private String name;

    private Integer age;

    private String nickName;

    private byte[] image;

    public static final String ID = "id";

}

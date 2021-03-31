package persion.bleg.mybatis.mp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 基础实体类，提供基础的实体类字段
 *
 * @author shiyuquan
 * @since 2019/12/23 11:27 上午
 */
@Getter
@Setter
@ToString
public class BaseEntity {

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String creator;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private String createTime;

    /** 修改人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String modifier;

    /** 修改时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String modifyTime;

}
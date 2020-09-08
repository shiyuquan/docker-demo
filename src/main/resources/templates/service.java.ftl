<#assign u_f_entity = "${entity?uncap_first}"/>
package ${package.Service};

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;
import ${package.Entity}.${entity};
import ${superServiceClassPackage};

import java.util.List;

/**
 * <p>
 * ${table.comment!} 服务类
 * </p>
 *
 * @author ${author}
 * @since ${cfg.formatDate}
 */
<#if kotlin>
interface ${table.serviceName} : ${superServiceClass}<${entity}>
<#else>
public interface ${table.serviceName} extends ${superServiceClass}<${entity}> {

    /**
     * 查询列表
     * @return {@link List} {@link ${entity}}
     */
    List<${entity}> select${entity}List();

    /**
     * 分页查询
     *
     * @param page 页数
     * @param size 页大小
     * @return {@link IPage} {@link ${entity}}
     */
    IPage<${entity}> select${entity}Page(Integer page, Integer size);

    /**
     * 根据id查询
     * @param id 主键
     * @return {@link ${entity}}
     */
    ${entity} selectById(Integer id);

    /**
     * 新增对象
     *
     * @param ${u_f_entity} ${u_f_entity}
     * @return 成功与否
     */
    boolean add${entity}(${entity} ${u_f_entity});

    /**
     * 修改对象
     * @param ${u_f_entity} ${u_f_entity}
     * @return {@link Boolean}
     */
    Boolean update${entity}ById(${entity} ${u_f_entity});

    /**
     * 根据id删除
     * @param id 主键
     * @return {@link Boolean}
     */
    Boolean deleteById(Integer id);
}
</#if>

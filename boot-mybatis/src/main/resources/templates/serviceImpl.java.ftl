<#assign u_f_entity = "${entity?uncap_first}"/>
package ${package.ServiceImpl};

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import ${package.Entity}.${entity};
import ${package.Mapper}.${table.mapperName};
import ${package.Service}.${table.serviceName};
import ${superServiceImplClassPackage};
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * ${table.comment!} 服务实现类
 * </p>
 *
 * @author ${author}
 * @since ${cfg.formatDate}
 */
@Slf4j
@Service
<#if kotlin>
open class ${table.serviceImplName} : ${superServiceImplClass}<${table.mapperName}, ${entity}>(), ${table.serviceName} {

}
<#else>
public class ${table.serviceImplName} extends ${superServiceImplClass}<${table.mapperName}, ${entity}> implements ${table.serviceName} {

    /**
     * 查询列表
     *
     * @return {@link List} {@link ${entity}}
     */
    @Override
    public List<${entity}> select${entity}List() {
        return baseMapper.selectList(null);
    }

    /**
     * 分页查询
     *
     * @param page 页数
     * @param size 页大小
     * @return {@link IPage} {@link ${entity}}
     */
    @Override
    public IPage<${entity}> select${entity}Page(Integer page, Integer size) {
    Page<${entity}> pageInfo = new Page<>(page, size, true);
        return baseMapper.selectPage(pageInfo, wrapper().orderByDesc(${entity}.ID));
    }

    /**
     * 根据id查询
     *
     * @param id 主键
     * @return {@link ${entity}}
     */
    @Override
    public ${entity} selectById(Long id) {
        return getOne(wrapper().eq(${entity}.ID, id));
    }

    /**
     * 新增对象
     *
     * @param ${u_f_entity} ${u_f_entity}
     * @return 成功与否
     */
    @Override
    public boolean add${entity}(${entity} ${u_f_entity}) {
        return save(${u_f_entity});
    }

    /**
     * 修改对象
     *
     * @param ${u_f_entity} ${u_f_entity}
     * @return {@link Boolean}
     */
    @Override
    public Boolean update${entity}ById(${entity} ${u_f_entity}) {
        return updateById(${u_f_entity});
    }

    /**
     * 根据id删除
     *
     * @param id 主键
     * @return {@link Boolean}
     */
    @Override
    public Boolean deleteById(Long id) {
        return removeById(id);
    }

}
</#if>

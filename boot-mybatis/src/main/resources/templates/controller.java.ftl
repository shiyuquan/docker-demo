<#assign service = "${table.serviceName?uncap_first}"/>
<#assign u_f_entity = "${entity?uncap_first}"/>
package ${package.Controller};

import org.springframework.web.bind.annotation.RequestMapping;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import persion.bleg.boot.base.IResult;
import persion.bleg.boot.base.Result;
import ${package.Entity}.${entity};
import ${package.Service}.${table.serviceName};

<#if restControllerStyle>
import org.springframework.web.bind.annotation.RestController;
<#else>
import org.springframework.stereotype.Controller;
</#if>
<#if superControllerClassPackage??>
import ${superControllerClassPackage};
</#if>

import java.util.List;

import static persion.bleg.boot.constant.DefalutConstant.DEFAULT_API_PREFIX;

/**
 * <p>
 * <#if table.comment??>${table.comment}<#else>${table.name}</#if> 前端控制器
 * </p>
 *
 * @author ${author}
 * @since ${cfg.formatDate}
 */
@Api(tags = "<#if table.comment??>${table.comment}<#else>${table.name}</#if>接口")
<#if restControllerStyle>
@RestController
<#else>
@Controller
</#if>
@RequestMapping(DEFAULT_API_PREFIX + "/<#if controllerMappingHyphenStyle??>${controllerMappingHyphen}<#else>${table.entityPath?replace("DO","")}</#if>")
<#if kotlin>
class ${table.controllerName}<#if superControllerClass??> : ${superControllerClass}()</#if>
<#else>
<#if superControllerClass??>
public class ${table.controllerName} extends ${superControllerClass} {
<#else>
public class ${table.controllerName} {
</#if>
    private ${table.serviceName} ${service};

    @Autowired
    public void set${table.serviceName}(${table.serviceName} ${service}) {
        this.${service} = ${service};
    }

    /**
     * 查询列表
     *
     * @return {@link List} {@link ${entity}}
     */
    @ApiOperation(value = "列表查询")
    @GetMapping(value = "/list")
    public IResult<List<${entity}>> select${entity}List() {
        return new Result<List<${entity}>>().success(${service}.select${entity}List());
    }

    /**
     * 分页查询
     *
     * @param page 页数
     * @param size 页大小
     * @return {@link IPage} {@link ${entity}}
     */
    @ApiOperation(value = "分页查询")
    @GetMapping(value = "/page")
    public IResult<IPage<${entity}>> select${entity}Page(@RequestParam Integer page, @RequestParam Integer size) {
        return new Result<IPage<${entity}>>().success(${service}.select${entity}Page(page, size));
    }

    /**
     * 根据id查询
     *
     * @param id 主键
     * @return {@link ${entity}}
     */
    @ApiOperation(value = "根据id查询")
    @GetMapping(value = "/{id}")
    public IResult<${entity}> selectById(@PathVariable("id") Long id) {
        return new Result<${entity}>().success(${service}.selectById(id));
    }

    /**
     * 新增对象
     *
     * @param ${u_f_entity} ${u_f_entity}
     * @return {@link Boolean}
     */
    @ApiOperation(value = "新增对象")
    @PostMapping(value = "")
    public IResult<Boolean> add(@RequestBody ${entity} ${u_f_entity}) {
        return new Result<Boolean>().success(${service}.add${entity}(${u_f_entity}));
    }

    /**
     * 修改对象
     *
     * @param ${u_f_entity} ${u_f_entity}
     * @return {@link Boolean}
     */
    @ApiOperation(value = "修改对象")
    @PutMapping(value = "")
    public IResult<Boolean> updateById(@RequestBody ${entity} ${u_f_entity}) {
        return new Result<Boolean>().success(${service}.update${entity}ById(${u_f_entity}));
    }

    /**
     * 根据id删除
     *
     * @param id 主键
     * @return {@link Boolean}
     */
    @ApiOperation(value = "根据id删除")
    @DeleteMapping(value = "/{id}")
    public IResult<Boolean> deleteById(@PathVariable("id") Long id) {
        return new Result<Boolean>().success(${service}.deleteById(id));
    }

}
</#if>

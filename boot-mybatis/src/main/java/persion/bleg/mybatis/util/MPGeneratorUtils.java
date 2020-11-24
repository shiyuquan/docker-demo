package persion.bleg.mybatis.util;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shiyuquan
 * @since 2020/9/7 7:01 下午
 */
@Slf4j
public class MPGeneratorUtils {

    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");
        String tableNames = "t_user";

        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        // 设置文件输出目录
        gc.setOutputDir(projectPath + "/src/main/java");
        // 设置开发人员
        gc.setAuthor("shiyuquan");
        // 是否打开输出目录
        gc.setOpen(false);
        gc.setActiveRecord(false);
        // 开启BaseResultMap
        gc.setBaseResultMap(true);
        // 开启baseColumnList
        gc.setBaseColumnList(true);
        // 时间类型对应策略
        gc.setDateType(DateType.TIME_PACK);
        // 是否在xml中添加二级缓存配置
        gc.setEnableCache(false);
        // 是否覆盖已有文件
        gc.setFileOverride(false);
        // 开启 Kotlin 模式
        gc.setKotlin(false);
        // 指定生成的主键的ID类型
        gc.setIdType(IdType.AUTO);
        // 实体属性 Swagger2 注解
        gc.setSwagger2(true);
        /**
         * 各层文件名称方式，例如： %sAction 生成 UserAction
         * %s 为占位符
         */
        gc.setControllerName("%sController");
        gc.setEntityName("%sDO");
        gc.setServiceName("%sService");
        gc.setServiceImplName("%sServiceImpl");
        gc.setMapperName("%sMapper");
        gc.setXmlName("%sMapper");
        mpg.setGlobalConfig(gc);

        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl("jdbc:mysql://localhost:3306/docker-demo");
        dsc.setDriverName("com.mysql.cj.jdbc.Driver");
        dsc.setUsername("root");
        dsc.setPassword("root");
        mpg.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        // 模块名称，设置之后会在父包名下创建文件夹
        pc.setModuleName("generator");
        // 父包名称
        pc.setParent("persion.bleg.dockerdemo.core");
        // 自定义包名
        // pc.setController();
        // pc.setEntity();
        // pc.setMapper();
        // pc.setService();
        // pc.setServiceImpl();
        // pc.setXml();
        mpg.setPackageInfo(pc);

        // 自定义配置
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                // 模板内通过 cfg.xxx 调用参数
                Map<String, Object> map = new HashMap<>();
                Date now = new Date(); // 创建一个Date对象，获取当前时间
                // 指定格式化格式 2019/12/23 11:19 上午
                SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd E h:mm:ss a");
                map.put("formatDate", f.format(now));
                this.setMap(map);
            }
        };
        mpg.setCfg(cfg);

        // 配置自定义模板
        TemplateConfig templateConfig = new TemplateConfig();
        templateConfig.setController("/templates/controller.java");
        templateConfig.setEntity("/templates/entity.java");
        templateConfig.setMapper("/templates/mapper.java");
        templateConfig.setXml("/templates/mapper.xml");
        templateConfig.setService("/templates/service.java");
        templateConfig.setServiceImpl("/templates/serviceImpl.java");

        // 数据库表配置
        StrategyConfig strategy = new StrategyConfig();
        // 数据库表映射到实体的命名策略
        strategy.setNaming(NamingStrategy.underline_to_camel);
        // 数据库表字段映射到实体的命名策略
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        // 是否跳过视图
        strategy.setSkipView(true);
        // 表前缀
        strategy.setTablePrefix("t_");
        // 自定义父类
        // strategy.setSuperEntityClass(BaseEntity.class);
        strategy.setSuperServiceImplClass("persion.bleg.dockerdemo.config.mp.IServiceImpl");
        strategy.setSuperServiceClass("com.baomidou.mybatisplus.extension.service.IService");
        strategy.setSuperMapperClass("com.baomidou.mybatisplus.core.mapper.BaseMapper");
        // 是否生成字段常量
        strategy.setEntityColumnConstant(true);
        // 是否为链式模型(构建者)
        strategy.setCapitalMode(false);
        // 设置lombok
        strategy.setEntityLombokModel(true);
        // 设置 controller 为rest风格接口
        strategy.setRestControllerStyle(true);
        // Boolean类型字段是否移除is前缀
        strategy.setEntityBooleanColumnRemoveIsPrefix(true);
        // 生成实体时，生成字段注解
        strategy.setEntityTableFieldAnnotationEnable(true);
        // 设置包含的表名
        strategy.setInclude(tableNames.split(","));

        mpg.setTemplate(templateConfig);
        mpg.setStrategy(strategy);
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }

}

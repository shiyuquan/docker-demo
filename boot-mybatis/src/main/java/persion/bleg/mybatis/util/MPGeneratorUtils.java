package persion.bleg.mybatis.util;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import persion.bleg.mybatis.mp.BaseEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * t_key_code_tree
 *
 * @author shiyuquan
 * @since 2020/9/7 7:01 下午
 */
@Slf4j
public class MPGeneratorUtils {

    /** 项目的根目录 */
    private static final String PROJECT_PATH = System.getProperty("user.dir");
    /** 文件输出目录 */
    private static final String OUTPUT_DIR = PROJECT_PATH + "/bleg-user/src/main/java";
    /** 包名，文件生成后会在输出目录生成包名文件夹 */
    private static final String PACKAGE = "core";
    /** 父包名 */
    private static final String PARENT_PACKAGE = "persion.bleg.user";

    /** 作者 */
    private static final String AUTHOR = "shiyuquan";

    /**
     * db配置
     */
    private static final String DB_URL = "jdbc:mysql://localhost:3306/docker-demo";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_USER_NAME = "root";
    private static final String DB_PWD = "root";

    /** 要生成的表，用逗号隔开 */
    private static final String TABLE_NAME = "t_button," + "t_menu," + "t_permission," + "t_role," + "t_user," + "t_user_group";
    /** 表钱缀 */
    private static final String TABLE_PREFIX = "t_";

    /** ServiceImpl 父类 */
    private static final String SUPER_SERVICE_IMPL = "persion.bleg.mybatis.mp.IServiceImpl";

    /** 是否需要公用父类 */
    private static final Boolean SUPER_ENTITY = false;

    public static void main(String[] args) {
        // String projectPath = System.getProperty("user.dir");
        // String tableNames = "t_user";

        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        // 设置文件输出目录
        gc.setOutputDir(OUTPUT_DIR);
        // 设置开发人员
        gc.setAuthor(AUTHOR);
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
        dsc.setUrl(DB_URL);
        dsc.setDriverName(DB_DRIVER);
        dsc.setUsername(DB_USER_NAME);
        dsc.setPassword(DB_PWD);
        mpg.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        // 模块名称，设置之后会在父包名下创建文件夹
        pc.setModuleName(PACKAGE);
        // 父包名称
        pc.setParent(PARENT_PACKAGE);
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
        strategy.setTablePrefix(TABLE_PREFIX);
        // 自定义父类

        if (SUPER_ENTITY) {
            strategy.setSuperEntityClass(BaseEntity.class);
        }
        strategy.setSuperServiceImplClass(SUPER_SERVICE_IMPL);
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
        strategy.setInclude(TABLE_NAME.split(","));

        mpg.setTemplate(templateConfig);
        mpg.setStrategy(strategy);
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }

}
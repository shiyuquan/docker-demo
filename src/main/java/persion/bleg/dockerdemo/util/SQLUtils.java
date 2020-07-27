package persion.bleg.dockerdemo.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * sql 工具类
 * @author shiyuquan
 * @since 2020/7/22 3:43 下午
 */
@Slf4j
public class SQLUtils {

    private SQLUtils() {}

    private static ThreadLocal<SimpleDateFormat> dateTimeFormater = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    /**
     * 获取 boundSql 的sql
     * @param boundSql boundSql
     * @return sql
     */
    public static String getSql(BoundSql boundSql) {
        String sql = boundSql.getSql();
        if (StringUtils.isEmpty(sql)) {
            return "";
        }
        return sql;
    }

    /**
     * 将 sql 的占位符替换成参数
     *
     * @param configuration configuration
     * @param boundSql boundSql
     * @return sql
     */
    public static String formateSql(Configuration configuration, BoundSql boundSql) {
        String sql = getSql(boundSql);
        // 美化sql
        sql = beautfulSql(sql);

        // 填充占位符，不考虑存储过程
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

        List<String> parameters = new ArrayList<>();
        if (CollectionUtils.isEmpty(parameterMappings)) {
            MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);
            for (int i = 0; i < parameterMappings.size(); i++) {
                ParameterMapping parameterMapping = parameterMappings.get(i);
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    // 参数
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    // 获取参数名
                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        // 获取参数值
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (parameterObject == null) {
                        return null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else {
                        value = metaObject == null ? null : metaObject.getValue(propertyName);
                    }

                    if (value instanceof Number) {
                        parameters.add(String.valueOf(value));
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("");
                        if (value instanceof Date) {
                            stringBuilder.append(dateTimeFormater.get().format((Date) value));
                        } else if (value instanceof String) {
                            stringBuilder.append(value);
                        }
                        stringBuilder.append("'");
                        parameters.add(stringBuilder.toString());
                    }

                }
            }
        }

        StringBuilder builder = new StringBuilder();
        if (!CollectionUtils.isEmpty(parameters)) {
            String[] sqlParts = sql.split("\\?");
            for (int i = 0; i < sqlParts.length; i++) {
                builder.append(sqlParts[i]);
                if (i <= parameters.size() - 1) {
                    builder.append(parameters.get(i));
                }
            }
        } else {
            builder.append(sql);
        }

        return builder.toString();
    }

    /**
     * 美化sql
     * @param sql sql
     * @return sql
     */
    public static String beautfulSql(String sql) {
        return sql.replaceAll("[\\s\n]+", " ");
    }

    /**
     * 格式化 sql 日志
     * @param sql sql
     * @param costTime 执行时间
     * @param obj 响应参数
     * @return log
     */
    public static String formatSqlLog(String sql, long costTime, Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nsql: \n");
        sb.append(com.alibaba.druid.sql.SQLUtils.formatMySql(sql));
        sb.append("\ntotal:");
        if (obj instanceof List) {
            List list = (List) obj;
            int count = list.size();
            sb.append(count);
        } else if (obj instanceof Integer) {
            sb.append(obj);
        }
        sb.append("\nexcute time:")
                .append(costTime)
                .append(" ms");
        return sb.toString();
    }
}

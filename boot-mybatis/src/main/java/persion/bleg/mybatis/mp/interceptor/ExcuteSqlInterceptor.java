package persion.bleg.mybatis.mp.interceptor;

import com.baomidou.mybatisplus.core.parser.SqlParserHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import persion.bleg.mybatis.util.SQLUtils;

import java.sql.Connection;

/**
 *
 * 该拦截器用于拦截执行的sql， 打印全sql和执行时间
 *
 * @author shiyuquan
 * @since 2020/7/22 3:23 下午
 */
@Slf4j
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class ExcuteSqlInterceptor extends SqlParserHelper implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        Configuration configuration = (Configuration) metaObject.getValue("delegate.configuration");

        Object result;
        String sqlId = mappedStatement.getId();
        long startTime = System.currentTimeMillis();
        result = invocation.proceed();
        long endTime = System.currentTimeMillis();
        long excuteTime = endTime - startTime;
        String sql = SQLUtils.formateSql(configuration, boundSql);
        log.info(SQLUtils.formatSqlLog(sql, excuteTime, result));
        return result;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
}

package persion.bleg.mybatis.mp.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
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
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
             @Signature(type = Executor.class, method = "query", args = {MappedStatement.class,
                                                                         Object.class,
                                                                         RowBounds.class,
                                                                         ResultHandler.class}),
             @Signature(type = Executor.class, method = "query", args = {MappedStatement.class,
                                                                         Object.class,
                                                                         RowBounds.class,
                                                                         ResultHandler.class,
                                                                         CacheKey.class,
                                                                         BoundSql.class}),
             @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class ExcuteSqlInterceptor implements Interceptor {

    private static ThreadLocal<String> tl = new ThreadLocal<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        StringBuilder sb = new StringBuilder();

        if (invocation.getTarget() instanceof StatementHandler) {
            StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

            BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
            Configuration configuration = (Configuration) metaObject.getValue("delegate.configuration");
            String sql = SQLUtils.formateSql(configuration, boundSql);
            tl.set(SQLUtils.formatSqlLog(sql));
        }


        Object result;
        if (invocation.getTarget() instanceof Executor) {
            long startTime = System.currentTimeMillis();
            result = invocation.proceed();
            long endTime = System.currentTimeMillis();
            long excuteTime = endTime - startTime;
            sb.append(tl.get()).append("\n").append(SQLUtils.formatSqlLog(excuteTime, result));
            log.info(sb.toString());
            return result;
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor || target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
}

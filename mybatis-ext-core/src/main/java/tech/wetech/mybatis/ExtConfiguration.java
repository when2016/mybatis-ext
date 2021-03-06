package tech.wetech.mybatis;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;
import tech.wetech.mybatis.dialect.Dialect;
import tech.wetech.mybatis.dialect.DialectClient;
import tech.wetech.mybatis.dialect.DialectType;
import tech.wetech.mybatis.mapper.Mapper;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * 增强配置类
 *
 * @author cjbi
 */
public class ExtConfiguration extends Configuration {

    protected final EntityMapperRegistry entityMapperRegistry = new EntityMapperRegistry(this);
    protected Dialect dialect = null;
    private boolean noAutoDialect;

    public ExtConfiguration() {
        super();
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setDialect(Class<? extends Dialect> dialect) {
        try {
            this.dialect = dialect.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot set Dialect.", e);
        }
    }

    public ExtConfiguration(Environment environment) {
        this();
        this.environment = environment;
    }

    @Override
    public <T> void addMapper(Class<T> type) {
        entityMapperRegistry.addMapper(type);
    }

    @Override
    public void addMappers(String packageName, Class<?> superType) {
        entityMapperRegistry.addMappers(packageName, superType);
    }

    @Override
    public void addMappers(String packageName) {
        entityMapperRegistry.addMappers(packageName);
    }

    @Override
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        return entityMapperRegistry.getMapper(type, sqlSession);
    }

    @Override
    public boolean hasMapper(Class<?> type) {
        return type == Mapper.class || entityMapperRegistry.hasMapper(type);
    }

    @Override
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        Executor executor = super.newExecutor(transaction, executorType);
        if (dialect == null) {
            dialect = getAutoDialect(transaction);
        }
        return dialect == null ? executor : new PagingExecutor(executor, dialect);
    }


    private Dialect getAutoDialect(Transaction transaction) {
        Dialect dialect = null;
        if (!noAutoDialect) {
            try {
                String url = transaction.getConnection().getMetaData().getURL();
                for (DialectType dialectType : DialectType.values()) {
                    if (url.toUpperCase().indexOf(String.format(":%s:", dialectType)) != -1) {
                        dialect = DialectClient.getDialect(dialectType);
                    }
                }
            } catch (SQLException | UnsupportedOperationException e) {
                noAutoDialect = true;
            }
        }
        return dialect;
    }
}

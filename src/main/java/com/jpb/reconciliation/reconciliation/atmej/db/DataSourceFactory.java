//package com.jpb.reconciliation.reconciliation.atmej.db;
//
//import com.jpb.reconciliation.reconciliation.atmej.config.AppConfig;
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
//
//import javax.sql.DataSource;
//
///**
// * Builds the production {@link DataSource} from an {@link AppConfig}.
// * HikariCP is used as the connection pool; its defaults are sensible but we
// * override sizing and timeouts from configuration.
// *
// * <p>The pool is configured with {@code autoCommit=false}, so callers must
// * either commit explicitly or accept the default rollback-on-close that
// * happens when a connection is returned to the pool.
// */
//public final class DataSourceFactory {
//
//    private DataSourceFactory() {}
//
//    public static DataSource create(AppConfig cfg) {
//        HikariConfig hc = new HikariConfig();
//        hc.setJdbcUrl(cfg.getRequired("db.url"));
//        hc.setUsername(cfg.getRequired("db.username"));
//        hc.setPassword(cfg.getRequired("db.password"));
//        hc.setDriverClassName(cfg.getOrDefault("db.driver", "oracle.jdbc.OracleDriver"));
//
//        hc.setMaximumPoolSize(cfg.getInt("db.pool.maxSize", 8));
//        hc.setMinimumIdle(cfg.getInt("db.pool.minIdle", 2));
//        hc.setConnectionTimeout(cfg.getLong("db.pool.connectionTimeoutMs", 30_000L));
//        hc.setIdleTimeout(cfg.getLong("db.pool.idleTimeoutMs", 600_000L));
//
//        hc.setPoolName("atm-ej-loader");
//        hc.setAutoCommit(false);
//
//        // Oracle-specific: prefetch helps batch loads.
//        hc.addDataSourceProperty("oracle.jdbc.implicitStatementCacheSize", "50");
//        hc.addDataSourceProperty("oracle.jdbc.defaultRowPrefetch",
//                String.valueOf(cfg.getInt("db.fetchSize", 500)));
//
//        return new HikariDataSource(hc);
//    }
//}

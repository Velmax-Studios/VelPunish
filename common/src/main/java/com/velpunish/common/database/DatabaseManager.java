package com.velpunish.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private final DatabaseConfig config;

    public DatabaseManager(DatabaseConfig config) {
        this.config = config;
    }

    public void connect() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(buildJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(hikariConfig);
    }

    private String buildJdbcUrl() {
        if ("postgresql".equalsIgnoreCase(config.getType())) {
            return "jdbc:postgresql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase();
        }
        return "jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

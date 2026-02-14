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

        if ("h2".equalsIgnoreCase(config.getType()) || "sqlite".equalsIgnoreCase(config.getType())) {
            hikariConfig.setDriverClassName("org.h2.Driver");
        }

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(hikariConfig);
    }

    private String buildJdbcUrl() {
        if ("postgresql".equalsIgnoreCase(config.getType())) {
            return "jdbc:postgresql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase();
        } else if ("h2".equalsIgnoreCase(config.getType()) || "sqlite".equalsIgnoreCase(config.getType())) {
            return "jdbc:h2:./plugins/VelPunish/database/" + config.getDatabase() + ";MODE=MySQL;AUTO_RECONNECT=TRUE";
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

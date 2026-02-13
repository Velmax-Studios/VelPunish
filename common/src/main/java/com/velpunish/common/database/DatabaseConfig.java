package com.velpunish.common.database;

public class DatabaseConfig {
    private String type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private int maxPoolSize;
    private int minIdle;
    private long connectionTimeout;

    public DatabaseConfig(String type, String host, int port, String database, String username, String password,
            int maxPoolSize, int minIdle, long connectionTimeout) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        this.connectionTimeout = connectionTimeout;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }
}

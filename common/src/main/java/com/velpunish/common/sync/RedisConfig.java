package com.velpunish.common.sync;

public class RedisConfig {
    private String host;
    private int port;
    private String password;
    private boolean enabled;

    public RedisConfig(String host, int port, String password, boolean enabled) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

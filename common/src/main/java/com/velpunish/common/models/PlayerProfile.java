package com.velpunish.common.models;

import java.util.UUID;

public class PlayerProfile {
    private final UUID uuid;
    private final String username;
    private final String latestIp;
    private final long lastLogin;

    public PlayerProfile(UUID uuid, String username, String latestIp, long lastLogin) {
        this.uuid = uuid;
        this.username = username;
        this.latestIp = latestIp;
        this.lastLogin = lastLogin;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getLatestIp() {
        return latestIp;
    }

    public long getLastLogin() {
        return lastLogin;
    }
}

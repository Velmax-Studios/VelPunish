package com.velpunish.common.utils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    // Map format: UUID -> (Command -> ExpiryTimestampMillis)
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public void setCooldown(UUID uuid, String command, int seconds) {
        if (seconds <= 0)
            return;
        long expiry = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(command, expiry);
    }

    public long getCooldownRemainingMillis(UUID uuid, String command) {
        ConcurrentHashMap<String, Long> userCooldowns = cooldowns.get(uuid);
        if (userCooldowns == null)
            return 0L;

        Long expiry = userCooldowns.get(command);
        if (expiry == null)
            return 0L;

        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            userCooldowns.remove(command);
            return 0L;
        }

        return remaining;
    }

    public boolean isOnCooldown(UUID uuid, String command) {
        return getCooldownRemainingMillis(uuid, command) > 0L;
    }
}

package com.velpunish.server;

import com.velpunish.common.cache.PunishmentCache;
import com.velpunish.common.database.DatabaseConfig;
import com.velpunish.common.database.DatabaseManager;
import com.velpunish.common.database.PunishmentRepository;
import com.velpunish.common.sync.RedisConfig;
import com.velpunish.common.sync.RedisManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VelPunishServer extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PunishmentRepository punishmentRepository;
    private PunishmentCache punishmentCache;
    private RedisManager redisManager;
    private boolean isFolia;

    @Override
    public void onEnable() {
        checkFolia();

        DatabaseConfig dbConfig = new DatabaseConfig("mysql", "localhost", 3306, "velpunish", "root", "password", 10, 2,
                30000);
        databaseManager = new DatabaseManager(dbConfig);
        databaseManager.connect();

        punishmentRepository = new PunishmentRepository(databaseManager);
        punishmentRepository.createTables().join();

        punishmentCache = new PunishmentCache();

        RedisConfig redisConfig = new RedisConfig("localhost", 6379, "", true);
        redisManager = new RedisManager(redisConfig);

        redisManager.setMessageHandler(message -> {
            String[] parts = message.split(":");
            if (parts.length >= 3 && "PUNISHMENT".equals(parts[0])) {
                try {
                    java.util.UUID targetUuid = java.util.UUID.fromString(parts[1]);
                    punishmentCache.invalidate(targetUuid);
                } catch (IllegalArgumentException ignored) {
                }
            }
        });

        redisManager.connect();

        getServer().getPluginManager().registerEvents(new com.velpunish.server.listeners.ChatListener(this), this);
        new com.velpunish.server.commands.CommandSystem(this);

        getLogger().info("VelPunish server plugin initialized successfully.");
    }

    @Override
    public void onDisable() {
        if (redisManager != null) {
            redisManager.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            this.isFolia = true;
            getLogger().info("Folia environment detected.");
        } catch (ClassNotFoundException e) {
            this.isFolia = false;
            getLogger().info("Standard Paper environment detected.");
        }
    }

    public boolean isFolia() {
        return isFolia;
    }

    public PunishmentRepository getPunishmentRepository() {
        return punishmentRepository;
    }

    public PunishmentCache getPunishmentCache() {
        return punishmentCache;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }
}

package com.velpunish.server;

import com.velpunish.common.cache.PunishmentCache;
import com.velpunish.common.database.DatabaseConfig;
import com.velpunish.common.database.DatabaseManager;
import com.velpunish.common.database.ProfileCache;
import com.velpunish.common.database.ProfileRepository;
import com.velpunish.common.database.PunishmentRepository;
import com.velpunish.common.sync.RedisConfig;
import com.velpunish.common.sync.RedisManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VelPunishServer extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PunishmentRepository punishmentRepository;
    private PunishmentCache punishmentCache;
    private ProfileRepository profileRepository;
    private ProfileCache profileCache;
    private RedisManager redisManager;
    private boolean isFolia;

    @Override
    public void onEnable() {
        checkFolia();

        DatabaseConfig dbConfig = new DatabaseConfig("h2", "localhost", 3306, "velpunish", "root", "password", 10, 2,
                30000);
        databaseManager = new DatabaseManager(dbConfig);
        databaseManager.connect();

        punishmentRepository = new PunishmentRepository(databaseManager);
        punishmentRepository.createTables().join();

        punishmentCache = new PunishmentCache();

        profileRepository = new ProfileRepository(databaseManager);
        profileRepository.createTables().join();

        profileCache = new ProfileCache(profileRepository);

        RedisConfig redisConfig = new RedisConfig("localhost", 6379, "", true);
        redisManager = new RedisManager(redisConfig);

        redisManager.setMessageHandler(message -> {
            String[] parts = message.split(":");
            if (parts.length >= 3 && "PUNISHMENT".equals(parts[0])) {
                try {
                    java.util.UUID targetUuid = java.util.UUID.fromString(parts[1]);
                    punishmentCache.invalidate(targetUuid);

                    org.bukkit.entity.Player player = getServer().getPlayer(targetUuid);
                    if (player != null) {
                        punishmentRepository
                                .getHistory(targetUuid,
                                        player.getAddress() != null ? player.getAddress().getAddress().getHostAddress()
                                                : "")
                                .thenAccept(history -> {
                                    punishmentCache.cacheHistory(history);
                                    history.getPunishments().stream()
                                            .filter(com.velpunish.common.models.Punishment::isActive)
                                            .filter(p -> p.getType().name().equals("BAN")
                                                    || p.getType().name().equals("KICK"))
                                            .findFirst()
                                            .ifPresent(p -> {
                                                Runnable disconnectTask = () -> {
                                                    player.kick(net.kyori.adventure.text.Component
                                                            .text("You have been punished!\n")
                                                            .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                                                            .append(net.kyori.adventure.text.Component
                                                                    .text("Reason: " + p.getReason())
                                                                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)));
                                                };

                                                if (isFolia) {
                                                    getServer().getRegionScheduler().execute(this, player.getLocation(),
                                                            disconnectTask);
                                                } else {
                                                    getServer().getScheduler().runTask(this, disconnectTask);
                                                }
                                            });
                                });
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        });

        redisManager.connect();

        getServer().getPluginManager().registerEvents(new com.velpunish.server.listeners.ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new com.velpunish.server.listeners.ProfileListener(this), this);
        new com.velpunish.server.commands.CommandSystem(this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.velpunish.server.hooks.VelPunishExpansion(this).register();
        }

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

    public ProfileRepository getProfileRepository() {
        return profileRepository;
    }

    public ProfileCache getProfileCache() {
        return profileCache;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }
}

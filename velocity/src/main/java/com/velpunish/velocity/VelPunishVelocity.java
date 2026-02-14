package com.velpunish.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velpunish.common.cache.PunishmentCache;
import com.velpunish.common.database.DatabaseConfig;
import com.velpunish.common.database.DatabaseManager;
import com.velpunish.common.database.ProfileCache;
import com.velpunish.common.database.ProfileRepository;
import com.velpunish.common.database.PunishmentRepository;
import com.velpunish.common.sync.RedisConfig;
import com.velpunish.common.sync.RedisManager;
import org.slf4j.Logger;

import java.util.UUID;

@Plugin(id = "velpunish", name = "VelPunish", version = "1.0.0", authors = { "VelMax Studios" })
public class VelPunishVelocity {

    private final ProxyServer server;
    private final Logger logger;

    private DatabaseManager databaseManager;
    private PunishmentRepository punishmentRepository;
    private PunishmentCache punishmentCache;
    private ProfileRepository profileRepository;
    private ProfileCache profileCache;
    private RedisManager redisManager;

    @Inject
    public VelPunishVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        DatabaseConfig dbConfig = new DatabaseConfig("mysql", "localhost", 3306, "velpunish", "root", "password", 10, 2,
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
                    UUID targetUuid = UUID.fromString(parts[1]);
                    punishmentCache.invalidate(targetUuid);

                    server.getPlayer(targetUuid).ifPresent(player -> {
                        punishmentRepository
                                .getHistory(targetUuid, player.getRemoteAddress().getAddress().getHostAddress())
                                .thenAccept(history -> {
                                    punishmentCache.cacheHistory(history);
                                    history.getPunishments().stream()
                                            .filter(com.velpunish.common.models.Punishment::isActive)
                                            .filter(p -> p.getType().name().equals("BAN")
                                                    || p.getType().name().equals("KICK"))
                                            .findFirst()
                                            .ifPresent(p -> {
                                                player.disconnect(net.kyori.adventure.text.Component
                                                        .text("You have been punished!\n")
                                                        .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                                                        .append(net.kyori.adventure.text.Component
                                                                .text("Reason: " + p.getReason())
                                                                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)));
                                            });
                                });
                    });
                } catch (IllegalArgumentException ignored) {
                }
            }
        });

        redisManager.connect();

        server.getEventManager().register(this, new com.velpunish.velocity.listeners.LoginListener(this));
        server.getEventManager().register(this, new com.velpunish.velocity.listeners.ProfileListener(this));
        new com.velpunish.velocity.commands.CommandSystem(this);

        logger.info("VelPunish velocity plugin initialized successfully.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (redisManager != null) {
            redisManager.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
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

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
import com.velpunish.common.database.PunishmentRepository;
import com.velpunish.common.sync.RedisConfig;
import com.velpunish.common.sync.RedisManager;
import org.slf4j.Logger;

@Plugin(id = "velpunish", name = "VelPunish", version = "1.0.0", authors = { "Aarav Roy" })
public class VelPunishVelocity {

    private final ProxyServer server;
    private final Logger logger;

    private DatabaseManager databaseManager;
    private PunishmentRepository punishmentRepository;
    private PunishmentCache punishmentCache;
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

        RedisConfig redisConfig = new RedisConfig("localhost", 6379, "", true);
        redisManager = new RedisManager(redisConfig);

        redisManager.setMessageHandler(message -> {
        });

        redisManager.connect();

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

    public RedisManager getRedisManager() {
        return redisManager;
    }
}

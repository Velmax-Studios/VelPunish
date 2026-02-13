package com.velpunish.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velpunish.common.models.History;
import com.velpunish.common.models.IPPunishment;
import com.velpunish.common.models.Punishment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PunishmentCache {
    private final Cache<UUID, History> historyCache;
    private final Cache<String, List<IPPunishment>> ipPunishmentCache;

    public PunishmentCache() {
        this.historyCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(5000)
                .build();

        this.ipPunishmentCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(5000)
                .build();
    }

    public void cacheHistory(History history) {
        historyCache.put(history.getUuid(), history);
        if (history.getIpPunishments() != null && !history.getIpPunishments().isEmpty()) {
            String ip = history.getIpPunishments().get(0).getIp();
            cacheIpPunishments(ip, history.getIpPunishments());
        }
    }

    public void cacheIpPunishments(String ip, List<IPPunishment> ipPunishments) {
        ipPunishmentCache.put(ip, new ArrayList<>(ipPunishments));
    }

    public Optional<History> getHistory(UUID uuid) {
        return Optional.ofNullable(historyCache.getIfPresent(uuid));
    }

    public Optional<List<IPPunishment>> getIpPunishments(String ip) {
        return Optional.ofNullable(ipPunishmentCache.getIfPresent(ip));
    }

    public void addPunishment(UUID uuid, Punishment punishment) {
        History history = historyCache.getIfPresent(uuid);
        if (history != null) {
            history.getPunishments().add(0, punishment);
            historyCache.put(uuid, history);
        }
    }

    public void addIpPunishment(String ip, IPPunishment punishment) {
        List<IPPunishment> punishments = ipPunishmentCache.getIfPresent(ip);
        if (punishments != null) {
            punishments.add(0, punishment);
            ipPunishmentCache.put(ip, punishments);
        } else {
            List<IPPunishment> newList = new ArrayList<>();
            newList.add(punishment);
            ipPunishmentCache.put(ip, newList);
        }
    }

    public void revokePunishment(UUID uuid, int id) {
        History history = historyCache.getIfPresent(uuid);
        if (history != null) {
            history.getPunishments().stream()
                    .filter(p -> p.getId() == id)
                    .forEach(p -> p.setActive(false));
            historyCache.put(uuid, history);
        }
    }

    public void revokeIpPunishment(String ip, int id) {
        List<IPPunishment> punishments = ipPunishmentCache.getIfPresent(ip);
        if (punishments != null) {
            punishments.stream()
                    .filter(p -> p.getId() == id)
                    .forEach(p -> p.setActive(false));
            ipPunishmentCache.put(ip, punishments);
        }
    }

    public void invalidate(UUID uuid) {
        historyCache.invalidate(uuid);
    }

    public void invalidateIp(String ip) {
        ipPunishmentCache.invalidate(ip);
    }

    public List<Punishment> getActivePunishments(UUID uuid) {
        History history = historyCache.getIfPresent(uuid);
        if (history == null) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        return history.getPunishments().stream()
                .filter(Punishment::isActive)
                .filter(p -> p.getEndTime() == -1 || p.getEndTime() > now)
                .collect(Collectors.toList());
    }

    public List<IPPunishment> getActiveIpPunishments(String ip) {
        List<IPPunishment> punishments = ipPunishmentCache.getIfPresent(ip);
        if (punishments == null) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        return punishments.stream()
                .filter(IPPunishment::isActive)
                .filter(p -> p.getEndTime() == -1 || p.getEndTime() > now)
                .collect(Collectors.toList());
    }
}

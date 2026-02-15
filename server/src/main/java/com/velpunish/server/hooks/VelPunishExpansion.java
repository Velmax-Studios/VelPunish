package com.velpunish.server.hooks;

import com.velpunish.common.models.IPPunishment;
import com.velpunish.common.models.Punishment;
import com.velpunish.server.VelPunishServer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class VelPunishExpansion extends PlaceholderExpansion {

    private final VelPunishServer plugin;

    public VelPunishExpansion(VelPunishServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "velpunish";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty() ? "VelMax Studios"
                : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        UUID uuid = player.getUniqueId();
        String ip = "";

        if (player.isOnline() && player.getPlayer() != null) {
            InetSocketAddress address = player.getPlayer().getAddress();
            ip = address != null && address.getAddress() != null ? address.getAddress().getHostAddress() : "";
        }

        if (params.equalsIgnoreCase("active_mutes")) {
            List<Punishment> punishments = plugin.getPunishmentCache().getActivePunishments(uuid);
            List<IPPunishment> ipPunishments = ip.isEmpty() ? List.of()
                    : plugin.getPunishmentCache().getActiveIpPunishments(ip);

            long activeMutes = punishments.stream().filter(p -> p.getType().name().equals("MUTE")).count() +
                    ipPunishments.stream().filter(p -> p.getType().name().equals("MUTE")).count();
            return String.valueOf(activeMutes);
        }

        if (params.equalsIgnoreCase("is_muted")) {
            List<Punishment> punishments = plugin.getPunishmentCache().getActivePunishments(uuid);
            List<IPPunishment> ipPunishments = ip.isEmpty() ? List.of()
                    : plugin.getPunishmentCache().getActiveIpPunishments(ip);

            boolean isMuted = punishments.stream().anyMatch(p -> p.getType().name().equals("MUTE")) ||
                    ipPunishments.stream().anyMatch(p -> p.getType().name().equals("MUTE"));
            return isMuted ? "Yes" : "No";
        }

        if (params.equalsIgnoreCase("total_punishments")) {
            return plugin.getPunishmentCache().getHistory(uuid)
                    .map(history -> String.valueOf(history.getPunishments().size())).orElse("0");
        }

        return null;
    }
}

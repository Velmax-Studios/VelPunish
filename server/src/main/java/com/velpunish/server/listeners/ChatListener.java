package com.velpunish.server.listeners;

import com.velpunish.common.models.IPPunishment;
import com.velpunish.common.models.Punishment;
import com.velpunish.server.VelPunishServer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class ChatListener implements Listener {

    private final VelPunishServer plugin;

    public ChatListener(VelPunishServer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        InetSocketAddress address = player.getAddress();
        String ip = address != null ? address.getAddress().getHostAddress() : "";

        plugin.getPunishmentRepository().getHistory(uuid, ip).thenAccept(history -> {
            plugin.getPunishmentCache().cacheHistory(history, ip);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        InetSocketAddress address = player.getAddress();
        String ip = address != null ? address.getAddress().getHostAddress() : "";

        List<Punishment> activePunishments = plugin.getPunishmentCache().getActivePunishments(uuid);
        for (Punishment punishment : activePunishments) {
            if (punishment.getType().name().equals("MUTE")) {
                handleMute(event, player, punishment.getReason(), punishment.getEndTime());
                return;
            }
        }

        List<IPPunishment> activeIpPunishments = plugin.getPunishmentCache().getActiveIpPunishments(ip);
        for (IPPunishment punishment : activeIpPunishments) {
            if (punishment.getType().name().equals("MUTE")) {
                handleMute(event, player, punishment.getReason(), punishment.getEndTime());
                return;
            }
        }
    }

    private void handleMute(AsyncChatEvent event, Player player, String reason, long endTime) {
        event.setCancelled(true);
        Component message = Component.text("You are muted!\n").color(NamedTextColor.RED)
                .append(Component.text("Reason: " + reason + "\n").color(NamedTextColor.GRAY));

        if (endTime == -1) {
            message = message.append(Component.text("Duration: Permanent").color(NamedTextColor.GRAY));
        } else {
            message = message
                    .append(Component.text("Expires in: " + formatDuration(endTime - System.currentTimeMillis()))
                            .color(NamedTextColor.GRAY));
        }

        player.sendMessage(message);
    }

    private String formatDuration(long millis) {
        if (millis <= 0)
            return "Expired";
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0)
            return days + " days, " + (hours % 24) + " hours";
        if (hours > 0)
            return hours + " hours, " + (minutes % 60) + " minutes";
        if (minutes > 0)
            return minutes + " minutes";
        return seconds + " seconds";
    }
}

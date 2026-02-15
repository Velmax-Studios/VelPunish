package com.velpunish.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velpunish.common.models.History;
import com.velpunish.common.models.IPPunishment;
import com.velpunish.common.models.Punishment;
import com.velpunish.velocity.VelPunishVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class LoginListener {

    private final VelPunishVelocity plugin;

    public LoginListener(VelPunishVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        try {
            History history = plugin.getPunishmentRepository().getHistory(uuid, ip).get();
            plugin.getPunishmentCache().cacheHistory(history, ip);

            List<Punishment> activePunishments = plugin.getPunishmentCache().getActivePunishments(uuid);
            for (Punishment punishment : activePunishments) {
                if (punishment.getType().name().equals("BAN")) {
                    event.setResult(LoginEvent.ComponentResult
                            .denied(buildBanMessage(punishment.getReason(), punishment.getEndTime())));
                    return;
                }
            }

            List<IPPunishment> activeIpPunishments = plugin.getPunishmentCache().getActiveIpPunishments(ip);
            for (IPPunishment punishment : activeIpPunishments) {
                if (punishment.getType().name().equals("BAN")) {
                    event.setResult(LoginEvent.ComponentResult
                            .denied(buildBanMessage(punishment.getReason(), punishment.getEndTime())));
                    return;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().error("Failed to load punishment history for " + player.getUsername(), e);
            event.setResult(LoginEvent.ComponentResult.denied(
                    Component.text("Failed to load your profile. Please try again.").color(NamedTextColor.RED)));
        }
    }

    private Component buildBanMessage(String reason, long endTime) {
        Component message = Component.text("You are banned from this network!\n").color(NamedTextColor.RED);
        message = message.append(Component.text("Reason: " + reason + "\n").color(NamedTextColor.GRAY));

        if (endTime == -1) {
            message = message.append(Component.text("Duration: Permanent").color(NamedTextColor.GRAY));
        } else {
            message = message
                    .append(Component.text("Expires in: " + formatDuration(endTime - System.currentTimeMillis()))
                            .color(NamedTextColor.GRAY));
        }
        return message;
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

package com.velpunish.server.listeners;

import com.velpunish.common.models.PlayerProfile;
import com.velpunish.server.VelPunishServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ProfileListener implements Listener {

    private final VelPunishServer plugin;

    public ProfileListener(VelPunishServer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "";

        PlayerProfile profile = new PlayerProfile(
                player.getUniqueId(),
                player.getName(),
                ip,
                System.currentTimeMillis());

        plugin.getProfileRepository().saveOrUpdateProfile(profile).thenRun(() -> {
            plugin.getProfileCache().addProfile(profile);
        });
    }
}

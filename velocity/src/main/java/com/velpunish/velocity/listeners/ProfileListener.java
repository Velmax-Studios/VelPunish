package com.velpunish.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velpunish.common.models.PlayerProfile;
import com.velpunish.velocity.VelPunishVelocity;

public class ProfileListener {

    private final VelPunishVelocity plugin;

    public ProfileListener(VelPunishVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        PlayerProfile profile = new PlayerProfile(
                player.getUniqueId(),
                player.getUsername(),
                ip,
                System.currentTimeMillis());

        plugin.getProfileRepository().saveOrUpdateProfile(profile).thenRun(() -> {
            plugin.getProfileCache().addProfile(profile);
        });
    }
}

package com.velpunish.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velpunish.common.models.Punishment;
import com.velpunish.common.models.PunishmentType;
import com.velpunish.velocity.VelPunishVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.Command;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.CloudInjectionModule;
import org.incendo.cloud.velocity.VelocityCommandManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

public class CommandSystem {

    private final VelPunishVelocity plugin;
    private final VelocityCommandManager<CommandSource> commandManager;

    public CommandSystem(VelPunishVelocity plugin) {
        this.plugin = plugin;
        this.commandManager = new VelocityCommandManager<>(
                plugin.getServer().getPluginManager().fromInstance(plugin).orElseThrow(),
                plugin.getServer(),
                ExecutionCoordinator.asyncCoordinator(),
                org.incendo.cloud.SenderMapper.identity());
        registerCommands();
    }

    private void registerCommands() {
        Command.Builder<CommandSource> banBuilder = commandManager.commandBuilder("ban", "velpunish.command.ban");
        commandManager.command(banBuilder
                .required("player", stringParser())
                .optional("reason", greedyStringParser(), DefaultValue.constant("The Ban Hammer has spoken!"))
                .handler(context -> {
                    String targetName = context.get("player");
                    String reason = context.getOrDefault("reason", "The Ban Hammer has spoken!");
                    CommandSource source = context.sender();

                    Optional<Player> targetOpt = plugin.getServer().getPlayer(targetName);
                    if (targetOpt.isEmpty()) {
                        source.sendMessage(Component.text("Player not found online.").color(NamedTextColor.RED));
                        return;
                    }

                    Player target = targetOpt.get();
                    UUID targetUuid = target.getUniqueId();
                    String targetIp = target.getRemoteAddress().getAddress().getHostAddress();
                    String operator = source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";

                    Punishment punishment = new Punishment(
                            0, targetUuid, targetIp, PunishmentType.BAN, reason, operator,
                            System.currentTimeMillis(), -1, true, "network");

                    plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                        plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                        plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                        target.disconnect(
                                Component.text("You are banned from this network!\n").color(NamedTextColor.RED)
                                        .append(Component.text("Reason: " + reason + "\n").color(NamedTextColor.GRAY))
                                        .append(Component.text("Duration: Permanent").color(NamedTextColor.GRAY)));

                        source.sendMessage(
                                Component.text("Banned " + targetName + " permanently.").color(NamedTextColor.GREEN));
                    });
                }));

        Command.Builder<CommandSource> kickBuilder = commandManager.commandBuilder("kick", "velpunish.command.kick");
        commandManager.command(kickBuilder
                .required("player", stringParser())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Kicked by an operator."))
                .handler(context -> {
                    String targetName = context.get("player");
                    String reason = context.getOrDefault("reason", "Kicked by an operator.");
                    CommandSource source = context.sender();

                    Optional<Player> targetOpt = plugin.getServer().getPlayer(targetName);
                    if (targetOpt.isEmpty()) {
                        source.sendMessage(Component.text("Player not found online.").color(NamedTextColor.RED));
                        return;
                    }

                    Player target = targetOpt.get();
                    UUID targetUuid = target.getUniqueId();
                    String targetIp = target.getRemoteAddress().getAddress().getHostAddress();
                    String operator = source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";

                    Punishment punishment = new Punishment(
                            0, targetUuid, targetIp, PunishmentType.KICK, reason, operator,
                            System.currentTimeMillis(), System.currentTimeMillis(), false, "network");

                    plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                        plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                        target.disconnect(
                                Component.text("You were kicked from this network!\n").color(NamedTextColor.RED)
                                        .append(Component.text("Reason: " + reason).color(NamedTextColor.GRAY)));

                        source.sendMessage(Component.text("Kicked " + targetName + ".").color(NamedTextColor.GREEN));
                    });
                }));

        Command.Builder<CommandSource> historyBuilder = commandManager.commandBuilder("history",
                "velpunish.command.history");
        commandManager.command(historyBuilder
                .required("player", stringParser())
                .handler(context -> {
                    String targetName = context.get("player");
                    CommandSource source = context.sender();

                    Optional<Player> targetOpt = plugin.getServer().getPlayer(targetName);
                    if (targetOpt.isEmpty()) {
                        source.sendMessage(Component.text(
                                "Player not found. (Offline lookup not implemented for history command via Cloud yet)")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    Player target = targetOpt.get();
                    UUID targetUuid = target.getUniqueId();
                    String targetIp = target.getRemoteAddress().getAddress().getHostAddress();

                    plugin.getPunishmentRepository().getHistory(targetUuid, targetIp).thenAccept(history -> {
                        source.sendMessage(
                                Component.text("History for " + targetName + ":").color(NamedTextColor.GOLD));
                        if (history.getPunishments().isEmpty()) {
                            source.sendMessage(Component.text("No history found.").color(NamedTextColor.GRAY));
                        } else {
                            history.getPunishments().forEach(p -> {
                                source.sendMessage(Component.text(
                                        "- [" + p.getType() + "] " + p.getReason() + " (Active: " + p.isActive() + ")")
                                        .color(NamedTextColor.YELLOW));
                            });
                        }
                    });
                }));
    }
}

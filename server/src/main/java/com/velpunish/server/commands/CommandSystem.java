package com.velpunish.server.commands;

import com.velpunish.common.models.Punishment;
import com.velpunish.common.models.PunishmentType;
import com.velpunish.server.VelPunishServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;

import java.util.UUID;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

public class CommandSystem {

    private final VelPunishServer plugin;
    private final PaperCommandManager<CommandSender> commandManager;

    public CommandSystem(VelPunishServer plugin) {
        this.plugin = plugin;
        this.commandManager = new PaperCommandManager<>(
                plugin,
                ExecutionCoordinator.asyncCoordinator(),
                org.incendo.cloud.SenderMapper.identity());

        if (this.commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            this.commandManager.registerBrigadier();
        } else if (this.commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            this.commandManager.registerAsynchronousCompletions();
        }

        registerCommands();
    }

    private void registerCommands() {
        Command.Builder<CommandSender> muteBuilder = commandManager.commandBuilder("mute", "velpunish.command.mute");
        commandManager.command(muteBuilder
                .required("player", stringParser())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Muted by an operator."))
                .handler(context -> {
                    String targetName = context.get("player");
                    String reason = context.getOrDefault("reason", "Muted by an operator.");
                    CommandSender source = context.sender();

                    Player target = plugin.getServer().getPlayerExact(targetName);
                    if (target == null) {
                        source.sendMessage(Component.text("Player not found online.").color(NamedTextColor.RED));
                        return;
                    }

                    UUID targetUuid = target.getUniqueId();
                    String targetIp = target.getAddress() != null ? target.getAddress().getAddress().getHostAddress()
                            : "";
                    String operator = source instanceof Player ? source.getName() : "CONSOLE";

                    Punishment punishment = new Punishment(
                            0, targetUuid, targetIp, PunishmentType.MUTE, reason, operator,
                            System.currentTimeMillis(), -1, true, "server");

                    plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                        plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                        plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                        target.sendMessage(Component.text("You have been muted!\n").color(NamedTextColor.RED)
                                .append(Component.text("Reason: " + reason + "\n").color(NamedTextColor.GRAY))
                                .append(Component.text("Duration: Permanent").color(NamedTextColor.GRAY)));

                        source.sendMessage(
                                Component.text("Muted " + targetName + " permanently.").color(NamedTextColor.GREEN));
                    });
                }));

        Command.Builder<CommandSender> historyBuilder = commandManager.commandBuilder("history",
                "velpunish.command.history");
        commandManager.command(historyBuilder
                .required("player", stringParser())
                .handler(context -> {
                    String targetName = context.get("player");
                    CommandSender source = context.sender();

                    Player target = plugin.getServer().getPlayerExact(targetName);
                    if (target == null) {
                        source.sendMessage(Component.text(
                                "Player not found. (Offline lookup not implemented for history command via Cloud yet)")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    UUID targetUuid = target.getUniqueId();
                    String targetIp = target.getAddress() != null ? target.getAddress().getAddress().getHostAddress()
                            : "";

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

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
import java.util.function.Consumer;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import org.incendo.cloud.component.CommandComponent;
import com.velpunish.common.commands.OfflinePlayerSuggestionProvider;
import com.velpunish.common.models.PlayerProfile;

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

    private void resolveTarget(String targetName, Consumer<PlayerProfile> callback, Runnable onNotFound) {
        plugin.getProfileCache().getProfile(targetName).thenAccept(profileOpt -> {
            if (profileOpt.isPresent()) {
                callback.accept(profileOpt.get());
            } else {
                onNotFound.run();
            }
        });
    }

    private void registerCommands() {
        Command.Builder<CommandSender> muteBuilder = commandManager.commandBuilder("mute", "velpunish.command.mute");
        commandManager.command(muteBuilder
                .required(CommandComponent.<CommandSender, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Muted by an operator."))
                .handler(context -> {
                    String targetName = context.get("player");
                    String reason = context.getOrDefault("reason", "Muted by an operator.");
                    CommandSender source = context.sender();

                    resolveTarget(targetName, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();
                        String operator = source instanceof Player ? source.getName() : "CONSOLE";

                        Punishment punishment = new Punishment(
                                0, targetUuid, targetIp, PunishmentType.MUTE, reason, operator,
                                System.currentTimeMillis(), -1, true, "server");

                        plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                            plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                            plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                            Player onlineTarget = plugin.getServer().getPlayer(targetUuid);
                            if (onlineTarget != null) {
                                onlineTarget.sendMessage(Component.text("You have been muted!\n")
                                        .color(NamedTextColor.RED)
                                        .append(Component.text("Reason: " + reason + "\n").color(NamedTextColor.GRAY))
                                        .append(Component.text("Duration: Permanent").color(NamedTextColor.GRAY)));
                            }

                            source.sendMessage(
                                    Component.text("Muted " + targetName + " permanently.")
                                            .color(NamedTextColor.GREEN));
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSender> historyBuilder = commandManager.commandBuilder("history",
                "velpunish.command.history");
        commandManager.command(historyBuilder
                .required(CommandComponent.<CommandSender, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .handler(context -> {
                    String targetName = context.get("player");
                    CommandSender source = context.sender();

                    resolveTarget(targetName, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();

                        plugin.getPunishmentRepository().getHistory(targetUuid, targetIp).thenAccept(history -> {
                            source.sendMessage(
                                    Component.text("History for " + targetName + ":").color(NamedTextColor.GOLD));
                            if (history.getPunishments().isEmpty()) {
                                source.sendMessage(Component.text("No history found.").color(NamedTextColor.GRAY));
                            } else {
                                history.getPunishments().forEach(p -> {
                                    source.sendMessage(Component.text(
                                            "- [" + p.getType() + "] " + p.getReason() + " (Active: " + p.isActive()
                                                    + ")")
                                            .color(NamedTextColor.YELLOW));
                                });
                            }
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSender> banBuilder = commandManager.commandBuilder("ban", "velpunish.command.ban");
        commandManager.command(banBuilder
                .required(CommandComponent.<CommandSender, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .optional("reason", greedyStringParser(), DefaultValue.constant("The Ban Hammer has spoken!"))
                .handler(context -> {
                    String targetName = context.get("player");
                    String reason = context.getOrDefault("reason", "The Ban Hammer has spoken!");
                    CommandSender source = context.sender();

                    resolveTarget(targetName, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();
                        String operator = source instanceof Player ? source.getName() : "CONSOLE";

                        Punishment punishment = new Punishment(
                                0, targetUuid, targetIp, PunishmentType.BAN, reason, operator,
                                System.currentTimeMillis(), -1, true, "server");

                        plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                            plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                            plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                Player onlineTarget = plugin.getServer().getPlayer(targetUuid);
                                if (onlineTarget != null) {
                                    onlineTarget.kick(Component.text("You are banned from this network!\n")
                                            .color(NamedTextColor.RED)
                                            .append(Component.text("Reason: " + reason + "\n")
                                                    .color(NamedTextColor.GRAY))
                                            .append(Component.text("Duration: Permanent").color(NamedTextColor.GRAY)));
                                }
                            });

                            source.sendMessage(
                                    Component.text("Banned " + targetName + " permanently.")
                                            .color(NamedTextColor.GREEN));
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSender> tempbanBuilder = commandManager.commandBuilder("tempban",
                "velpunish.command.tempban");
        commandManager.command(tempbanBuilder
                .required(CommandComponent.<CommandSender, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .required("duration", stringParser())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Temporarily Banned."))
                .handler(context -> {
                    String targetName = context.get("player");
                    String durationStr = context.get("duration");
                    String reason = context.getOrDefault("reason", "Temporarily Banned.");
                    CommandSender source = context.sender();

                    long expiry = parseDuration(durationStr);
                    if (expiry == -1L) {
                        source.sendMessage(Component.text("Invalid duration format. Use 1d, 1h, 30m, etc.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    resolveTarget(targetName, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();
                        String operator = source instanceof Player ? source.getName() : "CONSOLE";

                        Punishment punishment = new Punishment(
                                0, targetUuid, targetIp, PunishmentType.BAN, reason, operator,
                                System.currentTimeMillis(), expiry, true, "server");

                        plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                            plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                            plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                Player onlineTarget = plugin.getServer().getPlayer(targetUuid);
                                if (onlineTarget != null) {
                                    onlineTarget.kick(Component.text("You are temporarily banned from this network!\n")
                                            .color(NamedTextColor.RED)
                                            .append(Component.text("Reason: " + reason + "\n")
                                                    .color(NamedTextColor.GRAY))
                                            .append(Component.text("Duration: " + durationStr)
                                                    .color(NamedTextColor.GRAY)));
                                }
                            });

                            source.sendMessage(
                                    Component.text("Temporarily banned " + targetName + " for " + durationStr + ".")
                                            .color(NamedTextColor.GREEN));
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSender> kickBuilder = commandManager.commandBuilder("kick", "velpunish.command.kick");
        commandManager.command(kickBuilder
                .required(CommandComponent.<CommandSender, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Kicked by an operator."))
                .handler(context -> {
                    String targetName = context.get("player");
                    String reason = context.getOrDefault("reason", "Kicked by an operator.");
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
                            0, targetUuid, targetIp, PunishmentType.KICK, reason, operator,
                            System.currentTimeMillis(), System.currentTimeMillis(), false, "server");

                    plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                        plugin.getPunishmentCache().addPunishment(targetUuid, saved);

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            target.kick(Component.text("You were kicked from this network!\n").color(NamedTextColor.RED)
                                    .append(Component.text("Reason: " + reason).color(NamedTextColor.GRAY)));
                        });

                        source.sendMessage(Component.text("Kicked " + targetName + ".").color(NamedTextColor.GREEN));
                    });
                }));

        Command.Builder<CommandSender> unbanBuilder = commandManager.commandBuilder("unban", "velpunish.command.unban");
        commandManager.command(unbanBuilder
                .required(CommandComponent.<CommandSender, String>builder().name("player_or_id").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Unbanned."))
                .handler(context -> {
                    String identifier = context.get("player_or_id");
                    String reason = context.getOrDefault("reason", "Unbanned.");
                    CommandSender source = context.sender();

                    try {
                        int id = Integer.parseInt(identifier);
                        plugin.getPunishmentRepository().getPunishmentById(id).thenAccept(punishment -> {
                            if (punishment != null && punishment.getType() == PunishmentType.BAN
                                    && punishment.isActive()) {
                                punishment.setActive(false);
                                plugin.getPunishmentRepository().updatePunishment(punishment);
                                plugin.getPunishmentCache().invalidate(punishment.getUuid());
                                plugin.getRedisManager().publishMessage("PUNISHMENT:" + punishment.getUuid() + ":0");
                                source.sendMessage(
                                        Component.text("Unbanned ID #" + id + ".").color(NamedTextColor.GREEN));
                            } else {
                                source.sendMessage(Component.text("Active ban with ID #" + id + " not found.")
                                        .color(NamedTextColor.RED));
                            }
                        });
                        return;
                    } catch (NumberFormatException ignored) {
                    }

                    resolveTarget(identifier, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();

                        plugin.getPunishmentRepository().getHistory(targetUuid, targetIp).thenAccept(history -> {
                            boolean unbanned = false;
                            for (Punishment p : history.getPunishments()) {
                                if (p.isActive() && p.getType() == PunishmentType.BAN) {
                                    p.setActive(false);
                                    plugin.getPunishmentRepository().updatePunishment(p);
                                    unbanned = true;
                                }
                            }

                            if (unbanned) {
                                plugin.getPunishmentCache().invalidate(targetUuid);
                                plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":0");
                                source.sendMessage(
                                        Component.text("Unbanned " + identifier + ".").color(NamedTextColor.GREEN));
                            } else {
                                source.sendMessage(
                                        Component.text(identifier + " is not banned.").color(NamedTextColor.RED));
                            }
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSender> unmuteBuilder = commandManager.commandBuilder("unmute",
                "velpunish.command.unmute");
        commandManager.command(unmuteBuilder
                .required(CommandComponent.<CommandSender, String>builder().name("player_or_id").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Unmuted."))
                .handler(context -> {
                    String identifier = context.get("player_or_id");
                    String reason = context.getOrDefault("reason", "Unmuted.");
                    CommandSender source = context.sender();

                    try {
                        int id = Integer.parseInt(identifier);
                        plugin.getPunishmentRepository().getPunishmentById(id).thenAccept(punishment -> {
                            if (punishment != null && punishment.getType() == PunishmentType.MUTE
                                    && punishment.isActive()) {
                                punishment.setActive(false);
                                plugin.getPunishmentRepository().updatePunishment(punishment);
                                plugin.getPunishmentCache().invalidate(punishment.getUuid());
                                plugin.getRedisManager().publishMessage("PUNISHMENT:" + punishment.getUuid() + ":0");
                                source.sendMessage(
                                        Component.text("Unmuted ID #" + id + ".").color(NamedTextColor.GREEN));
                            } else {
                                source.sendMessage(Component.text("Active mute with ID #" + id + " not found.")
                                        .color(NamedTextColor.RED));
                            }
                        });
                        return;
                    } catch (NumberFormatException ignored) {
                    }

                    resolveTarget(identifier, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();

                        plugin.getPunishmentRepository().getHistory(targetUuid, targetIp).thenAccept(history -> {
                            boolean unmuted = false;
                            for (Punishment p : history.getPunishments()) {
                                if (p.isActive() && p.getType() == PunishmentType.MUTE) {
                                    p.setActive(false);
                                    plugin.getPunishmentRepository().updatePunishment(p);
                                    unmuted = true;
                                }
                            }

                            if (unmuted) {
                                plugin.getPunishmentCache().invalidate(targetUuid);
                                plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":0");
                                Player onlineTarget = plugin.getServer().getPlayer(targetUuid);
                                if (onlineTarget != null) {
                                    onlineTarget.sendMessage(
                                            Component.text("You have been unmuted.").color(NamedTextColor.GREEN));
                                }
                                source.sendMessage(
                                        Component.text("Unmuted " + identifier + ".").color(NamedTextColor.GREEN));
                            } else {
                                source.sendMessage(
                                        Component.text(identifier + " is not muted.").color(NamedTextColor.RED));
                            }
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSender> tempmuteBuilder = commandManager.commandBuilder("tempmute",
                "velpunish.command.tempmute");
        commandManager.command(tempmuteBuilder
                .required(CommandComponent.<CommandSender, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .required("duration", stringParser())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Temporarily Muted."))
                .handler(context -> {
                    String targetName = context.get("player");
                    String durationStr = context.get("duration");
                    String reason = context.getOrDefault("reason", "Temporarily Muted.");
                    CommandSender source = context.sender();

                    long expiry = parseDuration(durationStr);
                    if (expiry == -1L) {
                        source.sendMessage(Component.text("Invalid duration format. Use 1d, 1h, 30m, etc.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    resolveTarget(targetName, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();
                        String operator = source instanceof Player ? source.getName() : "CONSOLE";

                        Punishment punishment = new Punishment(
                                0, targetUuid, targetIp, PunishmentType.MUTE, reason, operator,
                                System.currentTimeMillis(), expiry, true, "server");

                        plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                            plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                            plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                            Player onlineTarget = plugin.getServer().getPlayer(targetUuid);
                            if (onlineTarget != null) {
                                onlineTarget.sendMessage(
                                        Component.text("You have been temporarily muted!\n").color(NamedTextColor.RED)
                                                .append(Component.text("Reason: " + reason + "\n")
                                                        .color(NamedTextColor.GRAY))
                                                .append(Component.text("Duration: " + durationStr)
                                                        .color(NamedTextColor.GRAY)));
                            }

                            source.sendMessage(
                                    Component.text("Temporarily Muted " + targetName + " for " + durationStr + ".")
                                            .color(NamedTextColor.GREEN));
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));
    }

    private long parseDuration(String input) {
        if (input == null || input.isEmpty())
            return -1L;
        long multiplier = 1000L;
        char lastChar = Character.toLowerCase(input.charAt(input.length() - 1));
        String numberPart = input;

        if (Character.isLetter(lastChar)) {
            numberPart = input.substring(0, input.length() - 1);
            switch (lastChar) {
                case 's':
                    multiplier = 1000L;
                    break;
                case 'm':
                    multiplier = 60000L;
                    break;
                case 'h':
                    multiplier = 3600000L;
                    break;
                case 'd':
                    multiplier = 86400000L;
                    break;
                case 'w':
                    multiplier = 604800000L;
                    break;
                case 'M':
                    multiplier = 2592000000L;
                    break;
                case 'y':
                    multiplier = 31536000000L;
                    break;
                default:
                    return -1L;
            }
        }

        try {
            long value = Long.parseLong(numberPart);
            return System.currentTimeMillis() + (value * multiplier);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}

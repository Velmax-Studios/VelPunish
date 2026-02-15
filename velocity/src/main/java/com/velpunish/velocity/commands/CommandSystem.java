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
import java.util.function.Consumer;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import org.incendo.cloud.component.CommandComponent;
import com.velpunish.common.commands.OfflinePlayerSuggestionProvider;
import com.velpunish.common.models.PlayerProfile;

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
        Command.Builder<CommandSource> banBuilder = commandManager.commandBuilder("ban", "velpunish.command.ban");
        commandManager.command(banBuilder
                .required(CommandComponent.<CommandSource, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .optional("reason", greedyStringParser(), DefaultValue.constant("The Ban Hammer has spoken!"))
                .handler(context -> {
                    String targetName = context.get("player");
                    String reason = context.getOrDefault("reason", "The Ban Hammer has spoken!");
                    CommandSource source = context.sender();

                    resolveTarget(targetName, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();
                        String operator = source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";

                        Punishment punishment = new Punishment(
                                0, targetUuid, targetIp, PunishmentType.BAN, reason, operator,
                                System.currentTimeMillis(), -1, true, "network");

                        plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                            plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                            plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                            plugin.getServer().getPlayer(targetUuid).ifPresent(p -> p.disconnect(
                                    Component.text("You are banned from this network!\n").color(NamedTextColor.RED)
                                            .append(Component.text("Reason: " + reason + "\n")
                                                    .color(NamedTextColor.GRAY))
                                            .append(Component.text("Duration: Permanent").color(NamedTextColor.GRAY))));

                            source.sendMessage(
                                    Component.text("Banned " + targetName + " permanently.")
                                            .color(NamedTextColor.GREEN));
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSource> kickBuilder = commandManager.commandBuilder("kick", "velpunish.command.kick");
        commandManager.command(kickBuilder
                .required(CommandComponent.<CommandSource, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
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
                .required(CommandComponent.<CommandSource, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .handler(context -> {
                    String targetName = context.get("player");
                    CommandSource source = context.sender();

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

        Command.Builder<CommandSource> unbanBuilder = commandManager.commandBuilder("unban", "velpunish.command.unban");
        commandManager.command(unbanBuilder
                .required(CommandComponent.<CommandSource, String>builder().name("player_or_id").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Unbanned."))
                .handler(context -> {
                    String identifier = context.get("player_or_id");
                    String reason = context.getOrDefault("reason", "Unbanned.");
                    CommandSource source = context.sender();

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

        Command.Builder<CommandSource> tempbanBuilder = commandManager.commandBuilder("tempban",
                "velpunish.command.tempban");
        commandManager.command(tempbanBuilder
                .required(CommandComponent.<CommandSource, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .required("duration", stringParser())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Temporarily Banned."))
                .handler(context -> {
                    String targetName = context.get("player");
                    String durationStr = context.get("duration");
                    String reason = context.getOrDefault("reason", "Temporarily Banned.");
                    CommandSource source = context.sender();

                    long expiry = parseDuration(durationStr);
                    if (expiry == -1L) {
                        source.sendMessage(Component.text("Invalid duration format. Use 1d, 1h, 30m, etc.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    resolveTarget(targetName, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();
                        String operator = source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";

                        Punishment punishment = new Punishment(
                                0, targetUuid, targetIp, PunishmentType.BAN, reason, operator,
                                System.currentTimeMillis(), expiry, true, "network");

                        plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                            plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                            plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                            plugin.getServer().getPlayer(targetUuid).ifPresent(p -> p.disconnect(
                                    Component.text("You are temporarily banned from this network!\n")
                                            .color(NamedTextColor.RED)
                                            .append(Component.text("Reason: " + reason + "\n")
                                                    .color(NamedTextColor.GRAY))
                                            .append(Component.text("Duration: " + durationStr)
                                                    .color(NamedTextColor.GRAY))));

                            source.sendMessage(
                                    Component.text("Temporarily Banned " + targetName + " for " + durationStr + ".")
                                            .color(NamedTextColor.GREEN));
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSource> unmuteBuilder = commandManager.commandBuilder("unmute",
                "velpunish.command.unmute");
        commandManager.command(unmuteBuilder
                .required(CommandComponent.<CommandSource, String>builder().name("player_or_id").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Unmuted."))
                .handler(context -> {
                    String identifier = context.get("player_or_id");
                    String reason = context.getOrDefault("reason", "Unmuted.");
                    CommandSource source = context.sender();

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
                                plugin.getServer().getPlayer(targetUuid).ifPresent(p -> p.sendMessage(
                                        Component.text("You have been unmuted.").color(NamedTextColor.GREEN)));
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

        Command.Builder<CommandSource> tempmuteBuilder = commandManager.commandBuilder("tempmute",
                "velpunish.command.tempmute");
        commandManager.command(tempmuteBuilder
                .required(CommandComponent.<CommandSource, String>builder().name("player").parser(stringParser())
                        .suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository()))
                        .build())
                .required("duration", stringParser())
                .optional("reason", greedyStringParser(), DefaultValue.constant("Temporarily Muted."))
                .handler(context -> {
                    String targetName = context.get("player");
                    String durationStr = context.get("duration");
                    String reason = context.getOrDefault("reason", "Temporarily Muted.");
                    CommandSource source = context.sender();

                    long expiry = parseDuration(durationStr);
                    if (expiry == -1L) {
                        source.sendMessage(Component.text("Invalid duration format. Use 1d, 1h, 30m, etc.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    resolveTarget(targetName, profile -> {
                        UUID targetUuid = profile.getUuid();
                        String targetIp = profile.getLatestIp();
                        String operator = source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";

                        Punishment punishment = new Punishment(
                                0, targetUuid, targetIp, PunishmentType.MUTE, reason, operator,
                                System.currentTimeMillis(), expiry, true, "network");

                        plugin.getPunishmentRepository().savePunishment(punishment).thenAccept(saved -> {
                            plugin.getPunishmentCache().addPunishment(targetUuid, saved);
                            plugin.getRedisManager().publishMessage("PUNISHMENT:" + targetUuid + ":" + saved.getId());

                            plugin.getServer().getPlayer(targetUuid).ifPresent(p -> p.sendMessage(
                                    Component.text("You have been temporarily muted!\n").color(NamedTextColor.RED)
                                            .append(Component.text("Reason: " + reason + "\n")
                                                    .color(NamedTextColor.GRAY))
                                            .append(Component.text("Duration: " + durationStr)
                                                    .color(NamedTextColor.GRAY))));

                            source.sendMessage(
                                    Component.text("Temporarily Muted " + targetName + " for " + durationStr + ".")
                                            .color(NamedTextColor.GREEN));
                        });
                    }, () -> source
                            .sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED)));
                }));

        Command.Builder<CommandSource> staffHistoryBuilder = commandManager.commandBuilder("staffhistory",
                "velpunish.command.staffhistory");
        commandManager.command(staffHistoryBuilder
                .required("operator", stringParser())
                .handler(context -> {
                    String operator = context.get("operator");
                    CommandSource source = context.sender();

                    plugin.getPunishmentRepository().getStaffHistory(operator).thenAccept(punishments -> {
                        source.sendMessage(
                                Component.text("History of punishments by " + operator + ":")
                                        .color(NamedTextColor.GOLD));
                        if (punishments.isEmpty()) {
                            source.sendMessage(Component.text("No history found.").color(NamedTextColor.GRAY));
                        } else {
                            punishments.forEach(p -> {
                                source.sendMessage(Component.text(
                                        "- [" + p.getType() + "] " + p.getReason() + " (Active: " + p.isActive()
                                                + ") on "
                                                + plugin.getProfileCache().getProfile(p.getUuid()).join()
                                                        .map(PlayerProfile::getUsername).orElse(p.getUuid().toString()))
                                        .color(NamedTextColor.YELLOW));
                            });
                        }
                    });
                }));

        Command.Builder<CommandSource> staffRollbackBuilder = commandManager.commandBuilder("staffrollback",
                "velpunish.command.staffrollback");
        commandManager.command(staffRollbackBuilder
                .required("operator", stringParser())
                .required("duration", stringParser())
                .handler(context -> {
                    String operator = context.get("operator");
                    String durationStr = context.get("duration");
                    CommandSource source = context.sender();

                    long millis = parseDurationMillis(durationStr);
                    if (millis == -1L) {
                        source.sendMessage(Component.text("Invalid duration format. Use 1d, 1h, 30m, etc.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    long sinceTime = System.currentTimeMillis() - millis;

                    plugin.getPunishmentRepository().rollbackStaffPunishments(operator, sinceTime)
                            .thenAccept(updated -> {
                                source.sendMessage(
                                        Component
                                                .text("Rolled back " + updated + " active punishments issued by "
                                                        + operator + " within the last " + durationStr + ".")
                                                .color(NamedTextColor.GREEN));
                            });
                }));
    }

    private long parseDurationMillis(String input) {
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
            return value * multiplier;
        } catch (NumberFormatException e) {
            return -1L;
        }
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

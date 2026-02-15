package com.velpunish.common.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class PermissionUtils {

    /**
     * Checks if the sender has the bypass permission for a specific command limit.
     */
    public static boolean hasLimitBypass(CommandSender sender, String command) {
        return sender.hasPermission("velpunish.limit." + command + ".bypass")
                || sender.hasPermission("velpunish.limit.*.bypass");
    }

    /**
     * Obtains the maximum duration limit for the command based on permissions.
     * Returns -1 if they have bypass or if no limit is defined (meaning they can
     * punish for any duration).
     * Calculates the highest limit if they have multiple.
     */
    public static long getMaxDurationMillis(CommandSender sender, String command) {
        if (hasLimitBypass(sender, command) || sender.hasPermission("*") || sender.isOp()) {
            return -1L; // Unlimited
        }

        long maxMillis = 0L;
        boolean foundLimit = false;

        String prefix = "velpunish.limit." + command + ".";

        for (PermissionAttachmentInfo perm : sender.getEffectivePermissions()) {
            String node = perm.getPermission();
            if (node.startsWith(prefix)) {
                String durationStr = node.substring(prefix.length());
                if (durationStr.equalsIgnoreCase("bypass"))
                    continue;

                long millis = parseDurationMillis(durationStr);
                if (millis > maxMillis) {
                    maxMillis = millis;
                }
                foundLimit = true;
            }
        }

        return foundLimit ? maxMillis : -1L; // If no limit perms, default to unlimited (they just need the command
                                             // perm)
    }

    /**
     * Checks if the sender has the bypass permission for a specific command
     * cooldown.
     */
    public static boolean hasCooldownBypass(CommandSender sender, String command) {
        return sender.hasPermission("velpunish.cooldown." + command + ".bypass")
                || sender.hasPermission("velpunish.cooldown.*.bypass");
    }

    /**
     * Obtains the lowest cooldown for the command based on permissions in seconds.
     * Returns 0 if they have bypass or no cooldown is defined.
     */
    public static int getCooldownSeconds(CommandSender sender, String command) {
        if (hasCooldownBypass(sender, command) || sender.hasPermission("*") || sender.isOp()) {
            return 0;
        }

        int minSeconds = Integer.MAX_VALUE;
        boolean foundCooldown = false;

        String prefix = "velpunish.cooldown." + command + ".";

        for (PermissionAttachmentInfo perm : sender.getEffectivePermissions()) {
            String node = perm.getPermission();
            if (node.startsWith(prefix)) {
                String secondsStr = node.substring(prefix.length());
                if (secondsStr.equalsIgnoreCase("bypass"))
                    continue;

                try {
                    int seconds = Integer.parseInt(secondsStr);
                    if (seconds < minSeconds) {
                        minSeconds = seconds;
                    }
                    foundCooldown = true;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return foundCooldown ? minSeconds : 0; // Default to 0 seconds if no cooldown perms
    }

    /**
     * Same logic as parseDurationMillis in CommandSystem.
     */
    public static long parseDurationMillis(String input) {
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
}

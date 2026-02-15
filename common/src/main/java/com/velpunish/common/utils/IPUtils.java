package com.velpunish.common.utils;

import java.util.ArrayList;
import java.util.List;

public class IPUtils {

    public static List<String> generateIpWildcards(String ip) {
        List<String> wildcards = new ArrayList<>();
        wildcards.add(ip);

        if (ip == null || ip.isEmpty() || ip.contains("*")) {
            return wildcards;
        }

        if (ip.contains(".")) {
            // IPv4
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                wildcards.add(parts[0] + "." + parts[1] + "." + parts[2] + ".*");
                wildcards.add(parts[0] + "." + parts[1] + ".*.*");
                wildcards.add(parts[0] + ".*.*.*");
                wildcards.add("*.*.*.*");
            }
        } else if (ip.contains(":")) {
            // IPv6 basic support
            String[] parts = ip.split(":");
            if (parts.length > 1) {
                StringBuilder wildcard = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    wildcard.append(parts[i]).append(":");
                }
                wildcard.append("*");
                wildcards.add(wildcard.toString());
            }
        }

        return wildcards;
    }

    public static boolean matchesWildcard(String targetIp, String wildcardIp) {
        if (targetIp == null || wildcardIp == null)
            return false;
        if (targetIp.equals(wildcardIp))
            return true;

        String regex = wildcardIp.replace(".", "\\.").replace("*", ".*");
        return targetIp.matches(regex);
    }
}

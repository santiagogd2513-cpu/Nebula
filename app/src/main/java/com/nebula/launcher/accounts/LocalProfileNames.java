package com.nebula.launcher.accounts;

import java.util.Set;
import java.util.TreeSet;

/** Defensive operations for the locally stored player-name set. */
public final class LocalProfileNames {
    private LocalProfileNames() {}

    public static TreeSet<String> sanitize(Set<String> source) {
        TreeSet<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (source == null) return result;
        for (String name : source) {
            if (name != null && name.matches("[A-Za-z0-9_]{3,16}")) result.add(name);
        }
        return result;
    }

    public static TreeSet<String> withAdded(Set<String> source, String name) {
        if (name == null || !name.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Invalid local player name");
        }
        TreeSet<String> result = sanitize(source);
        result.add(name);
        return result;
    }

    public static TreeSet<String> without(Set<String> source, String name) {
        TreeSet<String> result = sanitize(source);
        if (name != null) result.remove(name);
        return result;
    }

    public static String resolveActive(Set<String> source, String activeName) {
        TreeSet<String> names = sanitize(source);
        if (activeName != null) {
            for (String name : names) if (name.equalsIgnoreCase(activeName)) return name;
        }
        return names.isEmpty() ? "" : names.first();
    }
}

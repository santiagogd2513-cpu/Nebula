package com.nebula.launcher.install;

/** Validates archive entry names before any future mod/resource installation. */
public final class SafeArchivePath {
    private SafeArchivePath() {}

    public static String normalize(String entryName) {
        if (entryName == null || entryName.isEmpty() || entryName.length() > 1024
                || entryName.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid archive entry name");
        }
        String value = entryName.replace('\\', '/');
        if (value.startsWith("/") || value.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Absolute archive paths are not allowed");
        }
        String[] segments = value.split("/", -1);
        StringBuilder safe = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) continue;
            if ("..".equals(segment) || segment.indexOf(':') >= 0) {
                throw new IllegalArgumentException("Archive path traversal is not allowed");
            }
            if (safe.length() > 0) safe.append('/');
            safe.append(segment);
        }
        if (safe.length() == 0) throw new IllegalArgumentException("Archive path is empty");
        return safe.toString();
    }
}

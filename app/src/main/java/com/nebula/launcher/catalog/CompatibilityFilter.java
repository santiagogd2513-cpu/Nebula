package com.nebula.launcher.catalog;
/** Validates catalog filter inputs before making remote requests. */
public final class CompatibilityFilter {
    private CompatibilityFilter() {}
    public static boolean isSupportedVersion(String v) {
        return v!=null && v.matches("[0-9]+\\.[0-9]+(\\.[0-9]+)?");
    }
    public static boolean isSupportedLoader(String v) {
        return "any".equals(v)||"forge".equals(v)||"fabric".equals(v)||"quilt".equals(v)||"neoforge".equals(v);
    }
}
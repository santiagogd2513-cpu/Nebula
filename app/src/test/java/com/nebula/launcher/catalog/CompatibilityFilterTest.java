package com.nebula.launcher.catalog;

import org.junit.Test;

import static org.junit.Assert.*;

public class CompatibilityFilterTest {
    @Test public void acceptsMinecraftVersionForms() {
        assertTrue(CompatibilityFilter.isSupportedVersion("1.20.1"));
        assertTrue(CompatibilityFilter.isSupportedVersion("1.21"));
        assertTrue(CompatibilityFilter.isSupportedVersion("26.1"));
    }

    @Test public void rejectsMalformedVersions() {
        assertFalse(CompatibilityFilter.isSupportedVersion("latest"));
        assertFalse(CompatibilityFilter.isSupportedVersion("1.20.1-forge"));
        assertFalse(CompatibilityFilter.isSupportedVersion(" 1.20.1 "));
        assertFalse(CompatibilityFilter.isSupportedVersion(null));
    }

    @Test public void acceptsKnownLoadersOnly() {
        assertTrue(CompatibilityFilter.isSupportedLoader("forge"));
        assertTrue(CompatibilityFilter.isSupportedLoader("fabric"));
        assertTrue(CompatibilityFilter.isSupportedLoader("quilt"));
        assertTrue(CompatibilityFilter.isSupportedLoader("neoforge"));
        assertTrue(CompatibilityFilter.isSupportedLoader("any"));
        assertFalse(CompatibilityFilter.isSupportedLoader("unknown"));
        assertFalse(CompatibilityFilter.isSupportedLoader(null));
    }
}

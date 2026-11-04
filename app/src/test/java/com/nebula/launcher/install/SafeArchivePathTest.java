package com.nebula.launcher.install;

import org.junit.Test;
import static org.junit.Assert.*;

public class SafeArchivePathTest {
    @Test public void acceptsAndNormalizesRelativePaths() {
        assertEquals("mods/example.jar", SafeArchivePath.normalize("mods/example.jar"));
        assertEquals("assets/example.png", SafeArchivePath.normalize("./assets\\example.png"));
        assertEquals("config/mod.json", SafeArchivePath.normalize("config//mod.json"));
    }
    @Test public void rejectsTraversalAndAbsolutePaths() {
        assertInvalid("../outside.txt");
        assertInvalid("mods/../../outside.jar");
        assertInvalid("..\\outside.jar");
        assertInvalid("/data/local/file");
        assertInvalid("C:\\temp\\file.jar");
        assertInvalid("");
        assertInvalid(null);
    }
    private void assertInvalid(String value) {
        try { SafeArchivePath.normalize(value); fail("Expected unsafe path rejection"); }
        catch (IllegalArgumentException expected) { }
    }
}

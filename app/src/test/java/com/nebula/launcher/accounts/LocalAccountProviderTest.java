package com.nebula.launcher.accounts;

import org.junit.Test;

import static org.junit.Assert.*;

public class LocalAccountProviderTest {
    private final LocalAccountProvider provider = new LocalAccountProvider();

    @Test public void createsStableOfflineIdentity() {
        AccountSession first = provider.create("Nebula_Player");
        AccountSession second = provider.create("Nebula_Player");
        assertEquals(first.getPlayerUuid(), second.getPlayerUuid());
        assertEquals("Nebula_Player", first.getPlayerName());
        assertEquals(AccountSession.Provider.LOCAL, first.getProvider());
    }

    @Test public void rejectsInvalidPlayerNames() {
        assertInvalid(null);
        assertInvalid("");
        assertInvalid("ab");
        assertInvalid("name with spaces");
        assertInvalid("player-name");
        assertInvalid("this_name_is_far_too_long");
    }

    private void assertInvalid(String name) {
        try {
            provider.create(name);
            fail("Expected invalid player name to be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected: local profiles accept only Minecraft-style names.
        }
    }
}

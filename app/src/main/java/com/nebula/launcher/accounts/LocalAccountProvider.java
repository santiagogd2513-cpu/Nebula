package com.nebula.launcher.accounts;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Offline profile support. This does not authenticate with Mojang/Microsoft services. */
public final class LocalAccountProvider {
    public AccountSession create(String playerName) {
        if (playerName == null || !playerName.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Use a Minecraft player name (3–16 letters, numbers, or underscores).");
        }
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        return new AccountSession("local:" + uuid, playerName, uuid, AccountSession.Provider.LOCAL);
    }
}

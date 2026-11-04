package com.nebula.launcher.accounts;

import java.util.UUID;

/** Non-secret account metadata. Never place access or refresh tokens in this object. */
public final class AccountSession {
    public enum Provider { MICROSOFT, ELY_BY, LOCAL }

    private final String accountId;
    private final String playerName;
    private final UUID playerUuid;
    private final Provider provider;

    public AccountSession(String accountId, String playerName, UUID playerUuid, Provider provider) {
        if (accountId == null || accountId.trim().isEmpty()) throw new IllegalArgumentException("accountId is required");
        if (playerName == null || playerName.trim().isEmpty()) throw new IllegalArgumentException("playerName is required");
        if (playerUuid == null || provider == null) throw new IllegalArgumentException("uuid and provider are required");
        this.accountId = accountId;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.provider = provider;
    }

    public String getAccountId() { return accountId; }
    public String getPlayerName() { return playerName; }
    public UUID getPlayerUuid() { return playerUuid; }
    public Provider getProvider() { return provider; }
}

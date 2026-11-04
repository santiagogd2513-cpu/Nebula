package com.nebula.launcher.runtime;

import com.nebula.launcher.accounts.AccountSession;

/** Stable boundary between Nebula's UI and the Minecraft runtime implementation. */
public interface GameRuntime {
    enum State { NOT_INTEGRATED, READY, INSTALLING, RUNNING, FAILED }
    State getState();
    void launch(String minecraftVersion, AccountSession account) throws RuntimeException;
}

package com.nebula.launcher.runtime;

import com.nebula.launcher.accounts.AccountSession;

/**
 * Integration seam for MojoLauncher. Deliberately fails closed until the upstream
 * runtime modules and native submodules are actually vendored and wired into Gradle.
 */
public final class MojoRuntimeAdapter implements GameRuntime {
    @Override public State getState() { return State.NOT_INTEGRATED; }

    @Override public void launch(String minecraftVersion, AccountSession account) {
        throw new UnsupportedOperationException(
                "MojoLauncher runtime is not integrated yet; no game process was started.");
    }
}

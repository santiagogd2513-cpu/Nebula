# Upstream runtime integration — Phase 2

Nebula's Android UI and runtime are separate modules by design. The upstream project selected for integration is [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), branch `v3_openjdk`.

## Verified upstream layout

The upstream project is not a small Android library. Its Gradle build includes the `app_mojo` application module, `forge_installer`, `NoBlocklist`, and JNI binding modules for GLFW, SDL, and mojoexec. Those native projects are supplied as Git submodules. Its build instructions invoke `./gradlew :app_mojo:assembleDebug`.

## Integration rules

1. Import the upstream source and all required Git submodules at a pinned commit; do not copy only the Java application folder.
2. Preserve the LGPL-3.0 license, copyright notices, source-offer obligations, and each third-party component's license.
3. Keep upstream authentication and game-runtime code as the runtime implementation; Nebula should call it through a small adapter rather than duplicating launch internals.
4. Audit authentication logging before enabling any account flow. Tokens, authorization codes, and PKCE verifiers must never be logged.
5. Keep CurseForge credentials out of the APK. Any required key belongs in a protected service, not a client resource or BuildConfig constant.
6. Align Android Gradle Plugin, compile SDK, NDK, Java level, and native build configuration with the pinned upstream revision before attempting a build.

## Current status

The `GameRuntime` contract and a fail-closed `MojoRuntimeAdapter` are present in Nebula. The actual upstream runtime, native submodules, and game-launch implementation are **not yet imported**. The adapter intentionally refuses to launch instead of pretending the game is available.

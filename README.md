# Nebula Android Launcher

Nebula is an Android launcher project for Minecraft: Java Edition.

## Current milestone

The Android app currently includes:

- A dark blue launcher interface with Inicio, Instalar, Biblioteca, Cuentas, and Ajustes.
- A version-selection screen backed by Mojang's official version manifest.
- Modrinth v2 search for mods, modpacks, shaders, and resource packs.
- Local/offline player profiles and encrypted token storage groundwork.
- Automated unit tests, debug APK builds, and GitHub Releases.

## Runtime integration status

The upstream [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher) source is now pinned as a Git submodule at `vendor/MojoLauncher`, branch `v3_openjdk`, commit `417bc82cc3d505e6ade6901040ffd4238af2df45`. Its recursive native submodules are part of that upstream checkout.

**This is the source-import milestone, not a completed in-app runtime integration.** Nebula's current Android application is still a separate app module; its `MojoRuntimeAdapter` remains fail-closed. The Nebula UI does not yet launch the vendored runtime, and the Instalar screen does not yet install Minecraft or Forge. A dedicated workflow is being added to compile the pinned upstream runtime as an integration smoke test before merging its Android activities, services, resources, and native libraries into Nebula.

## Target

- Android
- Minecraft: Java Edition
- Initial runtime target: Minecraft 1.20.1 with Forge

## Build Nebula UI

GitHub Actions provisions JDK 17, Android API 35, Build Tools 35.0.0, and Gradle 8.9. It runs unit tests before building the Nebula debug APK.

## Licensing

MojoLauncher is LGPL-3.0. Its license and third-party notices are retained in the vendored submodule. Any eventual distribution of a combined build must preserve the applicable license notices and source obligations.

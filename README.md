# Nebula Android Launcher

Nebula is an Android launcher project intended to run Minecraft: Java Edition on supported Android devices.

## Current milestone: Android launcher shell and Modrinth search

The app currently includes a native Android interface with Inicio, Biblioteca, Cuentas, and Ajustes sections. The Library screen is connected to the Modrinth v2 search API for mods, modpacks, shaders, and resource packs, with Minecraft version filtering and a Forge category filter where applicable.

This is still an early development build. It does **not** yet launch Minecraft, authenticate Microsoft or Ely.by accounts, download or install catalog content, or verify mod dependencies. The Jugar button remains disabled until the MojoLauncher runtime is integrated and tested.

## Planned architecture

- Android launcher shell and navigation.
- MojoLauncher runtime integration, preserving upstream notices and license obligations.
- Microsoft, Ely.by, and local account providers with safe credential handling.
- Modrinth catalog plus protected CurseForge access.
- Mods, modpacks, shaders, and resource packs with exact Minecraft-version and loader compatibility checks.

## Target

- Android
- Minecraft: Java Edition
- Initial compatibility target: Minecraft 1.20.1 with Forge

## Build prerequisites

- Android SDK command-line tools with Android API 35 and Build Tools 35.0.0
- JDK 17
- Gradle 8.9

GitHub Actions currently provisions Gradle directly and runs unit tests before building the debug APK. A Gradle wrapper is not yet committed.

## Licensing

Before importing upstream launcher code, retain its license and copyright notices and document all third-party components. No upstream runtime code is represented as included in this milestone.

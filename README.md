# Nebula Android Launcher

Nebula is an Android launcher project intended to run Minecraft: Java Edition on supported Android devices.

## Current milestone: Android UI shell (0.1.0)

This commit adds the initial Android Gradle project and a native Java interface with Inicio, Biblioteca, Cuentas, and Ajustes sections. It is a UI scaffold only: it does **not** yet launch Minecraft, authenticate accounts, download content, or produce a tested APK.

## Planned architecture

- Android launcher shell and navigation.
- MojoLauncher/PojavLauncher runtime integration, preserving upstream notices and license obligations.
- Microsoft, Ely.by, and local account providers with safe credential handling.
- Modrinth catalog integration and protected CurseForge access.
- Mods, modpacks, shaders, and resource packs with exact Minecraft-version and loader compatibility checks.

## Target

- Android
- Minecraft: Java Edition
- Initial compatibility target: Minecraft 1.20.1

## Build prerequisites

- Android Studio or Android SDK command-line tools
- JDK 17
- Gradle 8.9 or a compatible Gradle installation

The Gradle wrapper will be added when the upstream build/tooling integration is settled. The source project has not yet been built or tested in this environment.

## Licensing

Before importing upstream launcher code, retain its license and copyright notices and document all third-party components. No upstream code is represented as included in this initial scaffold.

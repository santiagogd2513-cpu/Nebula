# Nebula Android Launcher

Nebula is an Android launcher project for the planned Minecraft: Java Edition runtime.

## Current milestone

The Android app currently includes:

- A dark blue launcher interface with Inicio, Biblioteca, Cuentas, and Ajustes.
- Modrinth v2 search for mods, modpacks, shaders, and resource packs.
- Minecraft version and loader preferences saved on-device.
- Loader-aware catalog filtering (Forge/Fabric/Quilt/NeoForge applies to mods and modpacks; shaders and resource packs do not receive a loader facet).
- Project result cards that open the corresponding Modrinth project page.
- Local/offline player profiles that can be created, listed, and removed.
- Encrypted token storage based on Android Keystore for future online account providers.
- Automated unit tests, debug APK builds, and GitHub Releases.

## Important limitations

This milestone does **not** yet launch Minecraft. The MojoLauncher runtime and native dependencies have not been imported, so Jugar remains disabled. Microsoft and Ely.by authentication are not implemented. Modrinth search is metadata-only: automatic downloads, dependency resolution, file verification, and installation are not enabled. CurseForge is not connected.

## Target

- Android
- Minecraft: Java Edition
- Initial runtime target: Minecraft 1.20.1 with Forge

## Build

GitHub Actions provisions JDK 17, Android API 35, Build Tools 35.0.0, and Gradle 8.9. It runs unit tests before building the debug APK and publishing a versioned GitHub Release. A Gradle wrapper is not yet committed.

## Licensing

Before importing MojoLauncher source, preserve its LGPL-3.0 license and all third-party notices. No upstream runtime code is represented as included in this milestone.

# Modrinth catalog

Nebula's Library screen uses the Modrinth v2 search API over HTTPS. It supports mods, modpacks, shaders, and resource packs, and allows the user to choose a Minecraft version and loader in Settings.

## Compatibility filtering

- Every search applies the selected Minecraft game version.
- Forge, Fabric, Quilt, or NeoForge category facets are applied to mods and modpacks.
- Shaders and resource packs omit the loader facet.
- Search results are metadata only. Opening a result navigates to its public Modrinth project page.

## Not yet implemented

- Version-file selection and download.
- Dependency resolution and conflict checks.
- File size limits, SHA-512 verification, and safe atomic installation.
- Modpack import/export.
- CurseForge API integration. Its API key must remain on a protected backend, never inside the APK.

Do not label catalog results as installed or installable until all version metadata, dependencies, and file hashes have been validated.

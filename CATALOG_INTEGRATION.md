# Modrinth catalog

Nebula's Library screen uses the Modrinth v2 API over HTTPS. It supports paginated popular-first browsing and search for mods, modpacks, shaders, and resource packs. Project cards show the supplied icon, name, and summary. The in-app detail screen reads project authors, declared dependency metadata, long description, and gallery images when present.

## Compatibility filtering

- Every search applies the selected Minecraft game version.
- Forge, Fabric, Quilt, or NeoForge category facets are applied to mods and modpacks.
- Shaders and resource packs omit the loader facet.
- Search and detail results are metadata only. The project page can still be opened on Modrinth from the detail screen.

## Not yet implemented

- Version-file selection and download.
- Safe extraction and installation. A tested archive-path validation helper is present as groundwork, but it is not connected to an installer.
- Dependency resolution and conflict checks.
- File size limits, SHA-512 verification, and safe atomic installation.
- Modpack import/export.
- CurseForge API integration. Its API key must remain on a protected backend, never inside the APK.

Do not label catalog results as installed or installable until all version metadata, dependencies, and file hashes have been validated.

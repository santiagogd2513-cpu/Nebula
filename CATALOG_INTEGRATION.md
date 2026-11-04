# Modrinth catalog milestone

Nebula now has an asynchronous Modrinth v2 search client for mods, modpacks, shaders and resource packs. It applies the exact Minecraft game-version facet and an optional loader facet. Requests use HTTPS, a fixed API host and connection/read timeouts.

## Current limits
- The Java client is reusable but the visible Library screen is not yet connected to it.
- Search results are metadata only. Downloading, dependency resolution, checksum verification and installation are not implemented.
- Before installation, validate the chosen version's game_versions and loaders metadata; search facets alone are not sufficient.
- CurseForge is not connected. Its API key must remain on a protected backend, never inside the APK.
- Do not present a result as installable until its version metadata and dependencies have been checked.

## Build automation
A GitHub Actions workflow assembles a debug APK on pushes and pull requests and uploads it as an artifact. A successful workflow is required before calling the APK build-verified.
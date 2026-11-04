# Modrinth catalog milestone

Nebula has an asynchronous Modrinth v2 search client connected to the Library screen. It supports mods, modpacks, shaders, and resource packs, applies the requested Minecraft game-version facet, and applies the Forge category facet to mods and modpacks only. Requests use HTTPS, a fixed API host, and connection/read timeouts.

## Current limits

- Search results are metadata only. Downloading, dependency resolution, checksum verification, and installation are not implemented.
- Before installation, validate the selected version's game_versions and loaders metadata; search facets alone are not sufficient.
- Shader and resource-pack searches do not apply a Forge loader facet.
- CurseForge is not connected. Its API key must remain on a protected backend, never inside the APK.
- Do not present a result as installable until its version metadata and dependencies have been checked.

## Build automation

The GitHub Actions workflow runs on pushes to main and manual workflow dispatch. It runs unit tests, builds a debug APK, then publishes a versioned GitHub Release only when all preceding steps succeed.

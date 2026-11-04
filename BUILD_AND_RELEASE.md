# Build and release automation

Every push to main (and manual workflow dispatch) runs unit tests, builds the Android debug APK, then creates a uniquely versioned GitHub Release and uploads the APK. Version names use 0.1.<GitHub Actions run number>; versionCode is also increased for each run. The release is marked as the latest release. A failed test or build prevents publishing.

The workflow requires the repository's GitHub Actions token to have Contents: write permission. This is declared with the workflow-level permissions block. The workflow does not publish an APK when compilation fails.

## Current product state

The Library screen is now connected to the Modrinth search client for Minecraft 1.20.1 / Forge. Search results are metadata-only; no download or installation action is exposed yet.

The Minecraft runtime is not integrated. Microsoft and Ely.by authentication are not enabled. CurseForge requires a protected API proxy and is not embedded in this Android app.
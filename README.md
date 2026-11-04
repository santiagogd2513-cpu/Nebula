# Nebula Android Launcher

Nebula is a single-install Android launcher for Minecraft: Java Edition.

## Current milestone

The project contains:

- Nebula's Android interface, including Inicio, Instalar, Biblioteca, Cuentas, and Ajustes.
- A version-selection screen backed by Mojang's official version manifest.
- Modrinth v2 search for mods, modpacks, shaders, and resource packs.
- Local/offline player profiles and encrypted token-storage groundwork.
- A pinned MojoLauncher runtime with recursive native submodules.
- GitHub Actions validation and automatic publication of one standalone APK asset.

## Runtime integration status

MojoLauncher is pinned as a Git submodule at `vendor/MojoLauncher`:

- Upstream branch: `v3_openjdk`
- Pinned commit: `417bc82cc3d505e6ade6901040ffd4238af2df45`

The unified build runs `.github/scripts/integrate_nebula.py`, packages Nebula's Android library into Mojo's host application, applies the Nebula application ID and branding, preserves the runtime's storage bootstrap, and includes its Java runtime, installer modules, JNI bindings, and native submodules.

The resulting build is **one APK with application ID `com.nebula.launcher`**. It does not require a separate MJ Launcher installation. Nebula's home screen opens first; the runtime's internal activity is opened inside the same app when continuing to the engine. The internal runtime screen is still based on MojoLauncher components, rebranded for Nebula.

### Remaining end-to-end validation

A successful GitHub Actions build proves that the merged application compiles and that the APK has Nebula's package identity. It does not, by itself, prove that Minecraft has been installed or launched on a physical Android device.

- Nebula's selected version and loader are currently saved in Nebula preferences; they are not yet automatically mapped to a Mojo runtime instance.
- The user may need to select/create the matching instance inside the integrated runtime.
- Microsoft account flow, Vanilla 1.20.1 installation, and Forge 1.20.1 launch still require device-level end-to-end testing.
- Modrinth catalog search is available, while full dependency-aware modpack installation remains a separate integration task.

## Target

- Android
- Minecraft: Java Edition
- Initial runtime target: Minecraft 1.20.1 with Forge

## Build and automatic release

GitHub Actions checks out MojoLauncher and all recursive native submodules, provisions JDK 17, Android SDK/NDK and Gradle 8.13, runs Nebula unit tests, applies the integration script, and builds `:app_mojo:assembleFullDebug`.

A push to `integration/mojo-runtime` or `main` publishes the **APK file directly** as the sole GitHub Release asset. Pull-request builds validate the APK but do not publish a release. The workflow artifact is also retained for 14 days.

## Licensing

MojoLauncher is LGPL-3.0. Its license and third-party notices are retained in the vendored submodule. Any distribution of the combined build must preserve the applicable license notices and source obligations.

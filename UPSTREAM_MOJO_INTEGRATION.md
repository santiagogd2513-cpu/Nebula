# MojoLauncher runtime integration

## Imported upstream source

Nebula now includes the upstream project as a pinned Git submodule:

- Path: `vendor/MojoLauncher`
- Repository: https://github.com/MojoLauncher/MojoLauncher
- Branch: `v3_openjdk`
- Commit: `417bc82cc3d505e6ade6901040ffd4238af2df45`
- Tree: `09f3b6fb57c296371de6b456ab1d93a4751898c2`

The upstream Gradle build contains the `app_mojo` application, `forge_installer`, `NoBlocklist`, and JNI binding modules for GLFW, SDL, and mojoexec. It also declares native Git submodules. Checkout must therefore use recursive submodules.

## What this milestone changes

- Adds the upstream repository as a Git submodule at a fixed commit.
- Preserves the upstream repository's own license and third-party notices in that checkout.
- Adds an independent CI smoke build for `:app_mojo:assembleDebug`.

## What is not integrated yet

Nebula remains a separate Android application. Its `MojoRuntimeAdapter` intentionally fails closed and does not launch the game. Minecraft version selection still does not download the game, and no Forge profile is installed. A successful upstream smoke build proves that the vendored engine compiles; it does not prove that Nebula can invoke it.

## Required next integration work

1. Run and fix the upstream smoke build with all recursive submodules.
2. Choose the app merge strategy: migrate Nebula screens into the Mojo application module, or refactor Mojo's application module into a library while preserving its application-level services, providers, activities, manifest placeholders, resources, and native packaging.
3. Wire Nebula's install-profile model to the real upstream version installer and Forge installer.
4. Wire account sessions without logging credentials or authorization material.
5. Connect the Play action to the real game activity and add device-level smoke testing.
6. Verify Minecraft 1.20.1 Vanilla first, then Forge 1.20.1, before enabling the button in production UI.

## Upstream build reference

The upstream README documents:

```bash
git submodule update --init --recursive
cd vendor/MojoLauncher
./gradlew :app_mojo:assembleDebug
```

The upstream README states that MojoLauncher supports Minecraft Java and mod loaders including Forge and Fabric. See https://github.com/MojoLauncher/MojoLauncher/tree/v3_openjdk.

## License and security

MojoLauncher is LGPL-3.0. Keep its copyright notices, license, source availability obligations, and every third-party license when distributing derivative builds. Do not embed CurseForge API secrets in the APK. Audit authentication logs before connecting any account flow.

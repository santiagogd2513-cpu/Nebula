# Nebula Chunk Optimizer

A small Forge mod targeting Minecraft Java 1.20.1 / Forge 47.4.5.

## What it does

On the logical server, while a player is moving, the mod asks Minecraft's normal asynchronous chunk pipeline to prepare one full chunk roughly two chunks ahead of the player's movement direction. It makes at most one new target request every 10 player ticks and remembers the last target per player to avoid repeating the same request. Per-player state is cleared when the player logs out.

## Important limits

- This is conservative server-side prefetch, not a replacement chunk engine.
- It may reduce some server-side wait when approaching new terrain, but does not guarantee faster client rendering or higher FPS.
- It does not force a remote server to send chunks outside its configured view distance.
- It does not raise Minecraft's hard render-distance limit to 128 chunks. Client/server view-distance settings and hardware remain authoritative.
- Prefetching can increase server CPU, memory, disk IO, and world-generation work. Install it only on worlds/servers where you have permission.

## Build

The isolated Gradle project uses Java 17 and ForgeGradle 6. GitHub Actions runs unit tests, builds/reobfuscates the mod, checks that the JAR contains `mods.toml`, and uploads the JAR plus SHA-256 checksum as a workflow artifact.

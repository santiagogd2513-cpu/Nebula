package com.nebula.chunkoptimizer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Uses the vanilla/Forge asynchronous server chunk pipeline; it creates no worker threads. */
@Mod.EventBusSubscriber(modid = NebulaChunkOptimizer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChunkPrefetchHandler {
    private static final int TICK_INTERVAL = 10;
    private static final int LEAD_CHUNKS = 2;
    private static final ConcurrentMap<UUID, Long> LAST_REQUESTED = new ConcurrentHashMap<>();

    private ChunkPrefetchHandler() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % TICK_INTERVAL != 0) return;

        var movement = player.getDeltaMovement();
        if (movement.horizontalDistanceSqr() < 0.0025D) return;

        ChunkPos current = player.chunkPosition();
        int[] target = PrefetchTargetPlanner.targetChunk(
            current.x, current.z, movement.x, movement.z, LEAD_CHUNKS
        );
        if (target == null) return;

        long targetKey = ChunkPos.asLong(target[0], target[1]);
        UUID playerId = player.getUUID();
        Long previous = LAST_REQUESTED.put(playerId, targetKey);
        if (previous != null && previous.longValue() == targetKey) return;

        var chunkSource = player.serverLevel().getChunkSource();
        if (!chunkSource.hasChunk(target[0], target[1])) {
            // create=true schedules normal asynchronous load/generation in Minecraft.
            chunkSource.getChunkFuture(target[0], target[1], ChunkStatus.FULL, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_REQUESTED.remove(event.getEntity().getUUID());
    }
}

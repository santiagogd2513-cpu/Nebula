package com.nebula.chunkoptimizer;

import net.minecraftforge.fml.common.Mod;

@Mod(NebulaChunkOptimizer.MOD_ID)
public final class NebulaChunkOptimizer {
    public static final String MOD_ID = "nebula_chunk_optimizer";

    public NebulaChunkOptimizer() {
        // The mod is intentionally lightweight; the Forge event subscriber owns the work.
    }
}

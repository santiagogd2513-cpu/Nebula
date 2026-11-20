package com.nebula.chunkoptimizer;

/** Deterministic movement helper, separated for unit testing. */
public final class PrefetchTargetPlanner {
    private PrefetchTargetPlanner() { }

    /** Returns {chunkX, chunkZ}, or null while the player is idle. */
    public static int[] targetChunk(int playerChunkX, int playerChunkZ,
                                    double motionX, double motionZ, int leadChunks) {
        if (leadChunks < 1) {
            throw new IllegalArgumentException("leadChunks must be at least 1");
        }
        int stepX = Double.compare(motionX, 0.0D);
        int stepZ = Double.compare(motionZ, 0.0D);
        if (stepX == 0 && stepZ == 0) return null;
        return new int[] { playerChunkX + stepX * leadChunks, playerChunkZ + stepZ * leadChunks };
    }
}

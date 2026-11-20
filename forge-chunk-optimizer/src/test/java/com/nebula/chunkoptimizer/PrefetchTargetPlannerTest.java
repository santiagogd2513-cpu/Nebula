package com.nebula.chunkoptimizer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PrefetchTargetPlannerTest {
    @Test
    public void plansAheadWhenMovingEast() {
        assertArrayEquals(new int[] {12, -4}, PrefetchTargetPlanner.targetChunk(10, -4, 0.8D, 0.0D, 2));
    }

    @Test
    public void plansAheadDiagonally() {
        assertArrayEquals(new int[] {3, 9}, PrefetchTargetPlanner.targetChunk(5, 7, -0.4D, 0.2D, 2));
    }

    @Test
    public void doesNothingWhenPlayerIsIdle() {
        assertNull(PrefetchTargetPlanner.targetChunk(5, 7, 0.0D, 0.0D, 2));
    }
}

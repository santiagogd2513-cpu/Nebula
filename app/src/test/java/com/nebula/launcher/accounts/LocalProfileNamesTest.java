package com.nebula.launcher.accounts;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import static org.junit.Assert.*;

public class LocalProfileNamesTest {
    @Test public void filtersInvalidAndDuplicateNamesIgnoringCase() {
        Set<String> result = LocalProfileNames.sanitize(new java.util.HashSet<>(Arrays.asList("Alex", "alex", "bad name", "ok", null)));
        assertEquals(1, result.size());
        assertTrue(result.contains("Alex"));
    }
    @Test public void resolvesExistingActiveNameOrFirstAvailable() {
        assertEquals("Beta", LocalProfileNames.resolveActive(new java.util.HashSet<>(Arrays.asList("Beta", "Alpha")), "beta"));
        assertEquals("Alpha", LocalProfileNames.resolveActive(new java.util.HashSet<>(Arrays.asList("Beta", "Alpha")), "missing"));
        assertEquals("", LocalProfileNames.resolveActive(Collections.emptySet(), null));
    }
    @Test public void addAndRemoveReturnDefensiveSets() {
        Set<String> added = LocalProfileNames.withAdded(Collections.singleton("Alex"), "Nova");
        assertEquals(2, added.size());
        Set<String> removed = LocalProfileNames.without(added, "alex");
        assertEquals(1, removed.size());
        assertTrue(removed.contains("Nova"));
    }
}

package com.taino.draw;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a derangement (nobody maps to self). Prefers a single random cycle (Sattolo).
 */
public final class Derangement {

    private Derangement() {
    }

    /**
     * @param n participants count, must be &gt;= 2
     * @return permutation p where p[i] is the receiver index for giver i; p[i] != i
     */
    public static int[] cycle(int n, SecureRandom random) {
        if (n < 2) {
            throw new IllegalArgumentException("n must be >= 2");
        }
        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        // Sattolo's algorithm: random cyclic permutation
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i); // 0..i-1, never i
            Collections.swap(order, i, j);
        }
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            int giver = order.get(i);
            int receiver = order.get((i + 1) % n);
            result[giver] = receiver;
        }
        return result;
    }

    public static boolean isValid(int[] perm) {
        if (perm == null || perm.length < 2) {
            return false;
        }
        boolean[] seen = new boolean[perm.length];
        for (int i = 0; i < perm.length; i++) {
            int v = perm[i];
            if (v < 0 || v >= perm.length || v == i || seen[v]) {
                return false;
            }
            seen[v] = true;
        }
        return true;
    }
}

package com.taino.draw;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DerangementTest {

    @Test
    void nobodyGetsSelf() {
        SecureRandom random = new SecureRandom();
        for (int n = 2; n <= 20; n++) {
            for (int t = 0; t < 50; t++) {
                int[] perm = Derangement.cycle(n, random);
                assertTrue(Derangement.isValid(perm), () -> "invalid " + Arrays.toString(perm));
                for (int i = 0; i < n; i++) {
                    assertTrue(perm[i] != i);
                }
            }
        }
    }

    @Test
    void coversAllReceivers() {
        SecureRandom seeded = new SecureRandom();
        seeded.setSeed(1L);
        int[] perm = Derangement.cycle(5, seeded);
        assertEquals(5, Arrays.stream(perm).distinct().count());
    }
}

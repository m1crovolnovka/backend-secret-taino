package com.taino.room;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomCodeGeneratorTest {

    @Test
    void generatesUniqueLookingCodes() {
        RoomCodeGenerator gen = new RoomCodeGenerator();
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String code = gen.next();
            assertEquals(8, code.length());
            assertTrue(code.chars().allMatch(c -> Character.isLetterOrDigit(c)));
            codes.add(code);
        }
        assertTrue(codes.size() > 90);
    }
}

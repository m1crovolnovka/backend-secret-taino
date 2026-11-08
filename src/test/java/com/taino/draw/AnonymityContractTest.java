package com.taino.draw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taino.room.RoomPhase;
import com.taino.wishlist.WishlistItemType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnonymityContractTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void assignmentJsonHasNoOwnerIdentityFields() throws Exception {
        var dto = new AnonymousAssignmentDtos.AnonymousAssignmentResponse(
                true,
                null,
                RoomPhase.DRAW_DONE,
                Instant.now(),
                "офис",
                50,
                100,
                "BYN",
                "тёплое",
                List.of(new AnonymousAssignmentDtos.AnonymousWishlistItem(
                        WishlistItemType.LINK,
                        "наушники",
                        null,
                        "https://example.com",
                        null,
                        UUID.randomUUID(),
                        "https://i.ibb.co/x/y.jpg"
                ))
        );

        String json = mapper.writeValueAsString(dto);
        JsonNode node = mapper.readTree(json);
        String raw = json.toLowerCase();

        assertFalse(raw.contains("userid") || raw.contains("user_id"));
        assertFalse(raw.contains("username"));
        assertFalse(raw.contains("telegram"));
        assertFalse(raw.contains("color"));
        assertFalse(raw.contains("receiver"));
        assertFalse(raw.contains("giver"));
        assertTrue(node.path("wishlist").isArray());
        assertTrue(node.path("available").asBoolean());
    }
}

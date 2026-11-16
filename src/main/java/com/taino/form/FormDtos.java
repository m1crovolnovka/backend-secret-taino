package com.taino.form;

import jakarta.validation.constraints.Size;

public final class FormDtos {

    private FormDtos() {
    }

    public record FormResponse(
            Long id,
            String displayName,
            String note,
            String clothingSize,
            String shoeSize,
            String wbPickup,
            String telegramLink,
            boolean openMode,
            boolean locked,
            boolean canEdit
    ) {
    }

    public record UpdateFormRequest(
            @Size(min = 1, max = 120) String displayName,
            @Size(max = 2000) String note,
            @Size(max = 32) String clothingSize,
            @Size(max = 32) String shoeSize,
            @Size(max = 500) String wbPickup,
            @Size(max = 500) String telegramLink
    ) {
    }
}

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
            boolean locked,
            boolean canEdit
    ) {
    }

    public record UpdateFormRequest(
            @Size(min = 1, max = 120) String displayName,
            @Size(max = 2000) String note,
            @Size(max = 32) String clothingSize,
            @Size(max = 32) String shoeSize
    ) {
    }
}

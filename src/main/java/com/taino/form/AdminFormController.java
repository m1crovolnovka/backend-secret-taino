package com.taino.form;

import com.taino.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rooms/{roomId}/members/{memberId}/form")
public class AdminFormController {

    private final FormService formService;

    public AdminFormController(FormService formService) {
        this.formService = formService;
    }

    @GetMapping
    public FormDtos.FormResponse get(@PathVariable Long roomId, @PathVariable Long memberId) {
        return formService.adminGet(SecurityUtils.currentUser().userId(), roomId, memberId);
    }

    @PutMapping
    public FormDtos.FormResponse update(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            @Valid @RequestBody FormDtos.UpdateFormRequest request
    ) {
        return formService.adminUpdate(SecurityUtils.currentUser().userId(), roomId, memberId, request);
    }
}

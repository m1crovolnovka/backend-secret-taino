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
@RequestMapping("/api/rooms/{roomId}/me/form")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    @GetMapping
    public FormDtos.FormResponse get(@PathVariable Long roomId) {
        return formService.getMine(SecurityUtils.currentUser().userId(), roomId);
    }

    @PutMapping
    public FormDtos.FormResponse update(@PathVariable Long roomId, @Valid @RequestBody FormDtos.UpdateFormRequest request) {
        return formService.updateMine(SecurityUtils.currentUser().userId(), roomId, request);
    }
}

package com.taino.changerequest;

import com.taino.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/me/change-requests")
public class ChangeRequestController {

    private final ChangeRequestService service;

    public ChangeRequestController(ChangeRequestService service) {
        this.service = service;
    }

    @GetMapping
    public ChangeRequestDtos.ListResponse list(@PathVariable Long roomId) {
        return service.listMine(SecurityUtils.currentUser().userId(), roomId);
    }

    @PostMapping
    public ChangeRequestDtos.ChangeRequestResponse create(
            @PathVariable Long roomId,
            @Valid @RequestBody ChangeRequestDtos.CreateRequest request
    ) {
        return service.create(SecurityUtils.currentUser().userId(), roomId, request);
    }
}

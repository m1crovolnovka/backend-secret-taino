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
@RequestMapping("/api/admin/rooms/{roomId}/change-requests")
public class AdminChangeRequestController {

    private final ChangeRequestService service;

    public AdminChangeRequestController(ChangeRequestService service) {
        this.service = service;
    }

    @GetMapping
    public ChangeRequestDtos.ListResponse list(@PathVariable Long roomId) {
        return service.listAdmin(SecurityUtils.currentUser().userId(), roomId);
    }

    @PostMapping("/{requestId}/decide")
    public ChangeRequestDtos.ChangeRequestResponse decide(
            @PathVariable Long roomId,
            @PathVariable Long requestId,
            @Valid @RequestBody ChangeRequestDtos.DecideRequest request
    ) {
        return service.decide(SecurityUtils.currentUser().userId(), roomId, requestId, request);
    }
}

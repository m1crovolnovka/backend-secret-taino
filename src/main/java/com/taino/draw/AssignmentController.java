package com.taino.draw;

import com.taino.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/assignment")
public class AssignmentController {

    private final AssignmentQueryService assignmentQueryService;
    private final GiftClaimService giftClaimService;

    public AssignmentController(AssignmentQueryService assignmentQueryService, GiftClaimService giftClaimService) {
        this.assignmentQueryService = assignmentQueryService;
        this.giftClaimService = giftClaimService;
    }

    @GetMapping
    public AnonymousAssignmentDtos.AnonymousAssignmentResponse get(@PathVariable Long roomId) {
        return assignmentQueryService.getMine(SecurityUtils.currentUser().userId(), roomId);
    }

    @PostMapping("/claims/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void claim(@PathVariable Long roomId, @PathVariable Long itemId) {
        giftClaimService.claim(SecurityUtils.currentUser().userId(), roomId, itemId);
    }

    @DeleteMapping("/claims/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unclaim(@PathVariable Long roomId, @PathVariable Long itemId) {
        giftClaimService.unclaim(SecurityUtils.currentUser().userId(), roomId, itemId);
    }
}

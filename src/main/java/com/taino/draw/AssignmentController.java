package com.taino.draw;

import com.taino.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/assignment")
public class AssignmentController {

    private final AssignmentQueryService assignmentQueryService;

    public AssignmentController(AssignmentQueryService assignmentQueryService) {
        this.assignmentQueryService = assignmentQueryService;
    }

    @GetMapping
    public AnonymousAssignmentDtos.AnonymousAssignmentResponse get(@PathVariable Long roomId) {
        return assignmentQueryService.getMine(SecurityUtils.currentUser().userId(), roomId);
    }
}

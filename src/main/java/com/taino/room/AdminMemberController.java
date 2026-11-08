package com.taino.room;

import com.taino.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rooms/{roomId}/members/{memberId}")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    public AdminMemberController(AdminMemberService adminMemberService) {
        this.adminMemberService = adminMemberService;
    }

    @PostMapping("/kick")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void kick(@PathVariable Long roomId, @PathVariable Long memberId) {
        adminMemberService.kick(SecurityUtils.currentUser().userId(), roomId, memberId);
    }
}

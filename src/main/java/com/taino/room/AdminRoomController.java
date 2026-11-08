package com.taino.room;

import com.taino.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rooms/{roomId}")
public class AdminRoomController {

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/participants")
    public RoomDtos.AdminParticipantsResponse participants(@PathVariable Long roomId) {
        return roomService.adminParticipants(SecurityUtils.currentUser().userId(), roomId);
    }
}

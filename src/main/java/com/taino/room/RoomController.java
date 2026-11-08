package com.taino.room;

import com.taino.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<RoomDtos.RoomSummary> myRooms() {
        return roomService.listMyRooms(SecurityUtils.currentUser().userId());
    }

    @PostMapping
    public RoomDtos.RoomDetail create(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.create(SecurityUtils.currentUser().userId(), request);
    }

    @PostMapping("/join")
    public RoomDtos.RoomDetail join(@Valid @RequestBody JoinRoomRequest request) {
        return roomService.join(SecurityUtils.currentUser().userId(), request);
    }

    @GetMapping("/{roomId}")
    public RoomDtos.RoomDetail get(@PathVariable Long roomId) {
        return roomService.get(SecurityUtils.currentUser().userId(), roomId);
    }

    @PutMapping("/{roomId}")
    public RoomDtos.RoomDetail update(@PathVariable Long roomId, @Valid @RequestBody UpdateRoomRequest request) {
        return roomService.update(SecurityUtils.currentUser().userId(), roomId, request);
    }
}

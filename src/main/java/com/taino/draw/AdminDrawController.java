package com.taino.draw;

import com.taino.security.SecurityUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/rooms/{roomId}/draw")
public class AdminDrawController {

    private final DrawService drawService;

    public AdminDrawController(DrawService drawService) {
        this.drawService = drawService;
    }

    @PostMapping
    public Map<String, Object> draw(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "false") boolean confirm,
            @RequestParam(defaultValue = "false") boolean redraw
    ) {
        if (!confirm) {
            return Map.of("ok", false, "code", "CONFIRM_REQUIRED", "message", "Передайте confirm=true");
        }
        drawService.adminDraw(SecurityUtils.currentUser().userId(), roomId, redraw);
        return Map.of("ok", true);
    }
}

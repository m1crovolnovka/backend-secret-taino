package com.taino.analytics;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/track")
public class TrackController {

    private final AnalyticsIngestService ingestService;

    public TrackController(AnalyticsIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @GetMapping("/r/{code}")
    public void redirect(
            @PathVariable String code,
            @RequestParam(defaultValue = "telegram") String to,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        AnalyticsDtos.TrackClickResponse click = ingestService.click(code, request);
        String target = "web".equalsIgnoreCase(to) ? click.redirectUrl() : click.telegramUrl();
        response.sendRedirect(target);
    }

    @GetMapping("/click/{code}")
    public AnalyticsDtos.TrackClickResponse click(@PathVariable String code, HttpServletRequest request) {
        return ingestService.click(code, request);
    }

    @PostMapping("/event")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void event(@Valid @RequestBody AnalyticsDtos.TrackEventRequest body, HttpServletRequest request) {
        ingestService.trackPublic(body, request);
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void open(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request
    ) {
        String code = body != null && body.get("code") != null ? String.valueOf(body.get("code")) : null;
        String visitorKey = body != null && body.get("visitorKey") != null ? String.valueOf(body.get("visitorKey")) : null;
        String referrer = body != null && body.get("referrer") != null ? String.valueOf(body.get("referrer")) : null;
        ingestService.trackPublic(
                new AnalyticsDtos.TrackEventRequest(AnalyticsEventType.MINIAPP_OPEN, code, visitorKey, referrer, body),
                request
        );
    }
}

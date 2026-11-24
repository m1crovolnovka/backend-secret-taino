package com.taino.analytics;

import com.taino.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;
    private final TrackingLinkService linkService;

    public AnalyticsController(AnalyticsQueryService queryService, TrackingLinkService linkService) {
        this.queryService = queryService;
        this.linkService = linkService;
    }

    @GetMapping("/overview")
    public AnalyticsDtos.OverviewResponse overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Range range = range(from, to);
        return queryService.overview(range.from(), range.to());
    }

    @GetMapping("/funnel")
    public AnalyticsDtos.FunnelResponse funnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Range range = range(from, to);
        return queryService.funnel(range.from(), range.to());
    }

    @GetMapping("/timeseries")
    public AnalyticsDtos.TimeseriesResponse timeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Range range = range(from, to);
        return queryService.timeseries(range.from(), range.to());
    }

    @GetMapping("/sources")
    public AnalyticsDtos.SourcesResponse sources(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Range range = range(from, to);
        return queryService.sources(range.from(), range.to());
    }

    @GetMapping("/links")
    public List<AnalyticsDtos.LinkResponse> links(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Range range = range(from, to);
        return linkService.list(range.from(), range.to());
    }

    @PostMapping("/links")
    public AnalyticsDtos.LinkResponse createLink(@Valid @RequestBody AnalyticsDtos.CreateLinkRequest request) {
        return linkService.create(SecurityUtils.currentStaff().userId(), request);
    }

    @PutMapping("/links/{id}")
    public AnalyticsDtos.LinkResponse updateLink(
            @PathVariable Long id,
            @Valid @RequestBody AnalyticsDtos.UpdateLinkRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Range range = range(from, to);
        return linkService.update(id, request, range.from(), range.to());
    }

    private static Range range(Instant from, Instant to) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(30, ChronoUnit.DAYS);
        if (!start.isBefore(end)) {
            start = end.minus(1, ChronoUnit.DAYS);
        }
        return new Range(start, end);
    }

    private record Range(Instant from, Instant to) {
    }
}

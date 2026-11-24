package com.taino.analytics;

import com.taino.common.ApiException;
import com.taino.common.TextSanitizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TrackingLinkService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TrackingLinkRepository linkRepository;
    private final StaffAccountRepository staffRepository;
    private final AnalyticsEventRepository eventRepository;
    private final AnalyticsIngestService ingestService;

    public TrackingLinkService(
            TrackingLinkRepository linkRepository,
            StaffAccountRepository staffRepository,
            AnalyticsEventRepository eventRepository,
            AnalyticsIngestService ingestService
    ) {
        this.linkRepository = linkRepository;
        this.staffRepository = staffRepository;
        this.eventRepository = eventRepository;
        this.ingestService = ingestService;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDtos.LinkResponse> list(Instant from, Instant to) {
        List<AnalyticsDtos.LinkResponse> out = new ArrayList<>();
        for (TrackingLinkEntity link : linkRepository.findAllByOrderByCreatedAtDesc()) {
            out.add(toDto(link, from, to));
        }
        return out;
    }

    @Transactional
    public AnalyticsDtos.LinkResponse create(Long staffId, AnalyticsDtos.CreateLinkRequest req) {
        String code = StringUtils.hasText(req.code())
                ? normalizeCode(req.code())
                : uniqueCode();
        if (linkRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "CODE_TAKEN", "Код ссылки уже занят");
        }
        TrackingLinkEntity link = new TrackingLinkEntity();
        link.setCode(code);
        link.setTitle(TextSanitizer.requirePlain(req.title(), 160, "Название"));
        link.setUtmSource(clean(req.utmSource(), 120));
        link.setUtmMedium(clean(req.utmMedium(), 120));
        link.setUtmCampaign(clean(req.utmCampaign(), 160));
        link.setUtmContent(clean(req.utmContent(), 160));
        link.setUtmTerm(clean(req.utmTerm(), 160));
        link.setLabel(clean(req.label(), 160));
        link.setActive(true);
        if (staffId != null) {
            staffRepository.findById(staffId).ifPresent(link::setCreatedBy);
        }
        link = linkRepository.save(link);
        Instant now = Instant.now();
        return toDto(link, now.minusSeconds(1), now.plusSeconds(1));
    }

    @Transactional
    public AnalyticsDtos.LinkResponse update(Long id, AnalyticsDtos.UpdateLinkRequest req, Instant from, Instant to) {
        TrackingLinkEntity link = linkRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "LINK_NOT_FOUND", "Ссылка не найдена"));
        if (StringUtils.hasText(req.title())) {
            link.setTitle(TextSanitizer.requirePlain(req.title(), 160, "Название"));
        }
        if (req.utmSource() != null) link.setUtmSource(clean(req.utmSource(), 120));
        if (req.utmMedium() != null) link.setUtmMedium(clean(req.utmMedium(), 120));
        if (req.utmCampaign() != null) link.setUtmCampaign(clean(req.utmCampaign(), 160));
        if (req.utmContent() != null) link.setUtmContent(clean(req.utmContent(), 160));
        if (req.utmTerm() != null) link.setUtmTerm(clean(req.utmTerm(), 160));
        if (req.label() != null) link.setLabel(clean(req.label(), 160));
        if (req.active() != null) link.setActive(req.active());
        link = linkRepository.save(link);
        return toDto(link, from, to);
    }

    private AnalyticsDtos.LinkResponse toDto(TrackingLinkEntity link, Instant from, Instant to) {
        long clicks = eventRepository.countByEventTypeAndLinkIdAndCreatedAtBetween(
                AnalyticsEventType.LINK_CLICK, link.getId(), from, to);
        long opens = eventRepository.countByEventTypeAndLinkIdAndCreatedAtBetween(
                AnalyticsEventType.MINIAPP_OPEN, link.getId(), from, to);
        long users = eventRepository.countByEventTypeAndLinkIdAndCreatedAtBetween(
                AnalyticsEventType.USER_REGISTER, link.getId(), from, to);
        String start = "trk_" + link.getCode();
        return new AnalyticsDtos.LinkResponse(
                link.getId(),
                link.getCode(),
                link.getTitle(),
                link.getUtmSource(),
                link.getUtmMedium(),
                link.getUtmCampaign(),
                link.getUtmContent(),
                link.getUtmTerm(),
                link.getLabel(),
                link.isActive(),
                start,
                ingestService.buildTelegramUrl(link.getCode()),
                ingestService.buildTrackUrl(link.getCode()),
                link.getCreatedAt(),
                clicks,
                opens,
                users
        );
    }

    private String uniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = randomCode(6);
            if (!linkRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_GEN", "Не удалось сгенерировать код");
    }

    private static String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String normalizeCode(String raw) {
        String code = TextSanitizer.requirePlain(raw, 32, "Код").toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]{3,32}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_CODE", "Код: 3–32 символа A-Z 0-9 _ -");
        }
        return code;
    }

    private static String clean(String value, int max) {
        return TextSanitizer.plain(value, max);
    }
}

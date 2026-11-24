package com.taino.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taino.common.TextSanitizer;
import com.taino.config.TainoProperties;
import com.taino.user.UserEntity;
import com.taino.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AnalyticsIngestService {

    private final AnalyticsEventRepository eventRepository;
    private final TrackingLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final TainoProperties properties;
    private final ObjectMapper objectMapper;

    public AnalyticsIngestService(
            AnalyticsEventRepository eventRepository,
            TrackingLinkRepository linkRepository,
            UserRepository userRepository,
            TainoProperties properties,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalyticsDtos.TrackClickResponse click(String code, HttpServletRequest request) {
        TrackingLinkEntity link = requireActiveLink(code);
        record(AnalyticsEventType.LINK_CLICK, link, null, null, request, Map.of());
        return new AnalyticsDtos.TrackClickResponse(
                link.getCode(),
                buildMiniAppRedirect(link.getCode(), link),
                buildTelegramUrl(link)
        );
    }

    @Transactional
    public void trackPublic(AnalyticsDtos.TrackEventRequest req, HttpServletRequest request) {
        String type = normalizeType(req.type());
        if (!AnalyticsEventType.MINIAPP_OPEN.equals(type) && !AnalyticsEventType.LINK_CLICK.equals(type)) {
            // public endpoint only allows open/click style beacons
            type = AnalyticsEventType.MINIAPP_OPEN;
        }
        TrackingLinkEntity link = resolveLink(req.code()).orElse(null);
        record(type, link, null, req.visitorKey(), request, req.meta());
    }

    @Transactional
    public void attributeFromStartParam(Long userId, String startParam) {
        resolveLinkFromStartParam(startParam).ifPresent(link -> attributeUser(userId, link));
    }

    @Transactional
    public void trackAuthenticated(Long userId, String startParam, String visitorKey, HttpServletRequest request) {
        Optional<TrackingLinkEntity> link = resolveLinkFromStartParam(startParam);
        link.ifPresent(l -> attributeUser(userId, l));
        record(
                AnalyticsEventType.MINIAPP_OPEN,
                link.orElse(null),
                userId,
                visitorKey,
                request,
                Map.of("startParam", startParam == null ? "" : startParam)
        );
    }

    @Transactional
    public void emit(String eventType, Long userId, String startParamOrCode, Map<String, Object> meta) {
        TrackingLinkEntity link = resolveLinkFromStartParam(startParamOrCode)
                .or(() -> resolveLink(startParamOrCode))
                .orElseGet(() -> userAttributionLink(userId));
        record(eventType, link, userId, null, null, meta == null ? Map.of() : meta);
    }

    @Transactional
    public void emitForUser(String eventType, Long userId, Map<String, Object> meta) {
        TrackingLinkEntity link = userAttributionLink(userId);
        record(eventType, link, userId, null, null, meta == null ? Map.of() : meta);
    }

    private void attributeUser(Long userId, TrackingLinkEntity link) {
        if (userId == null || link == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getAttributionLinkId() == null) {
                user.setAttributionLinkId(link.getId());
                userRepository.save(user);
            }
        });
    }

    private TrackingLinkEntity userAttributionLink(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(UserEntity::getAttributionLinkId)
                .flatMap(linkRepository::findById)
                .orElse(null);
    }

    private void record(
            String eventType,
            TrackingLinkEntity link,
            Long userId,
            String visitorKey,
            HttpServletRequest request,
            Map<String, Object> meta
    ) {
        AnalyticsEventEntity event = new AnalyticsEventEntity();
        event.setEventType(eventType);
        event.setLink(link);
        event.setUserId(userId);
        if (StringUtils.hasText(visitorKey)) {
            event.setVisitorKey(TextSanitizer.plain(visitorKey, 64));
        }
        if (request != null) {
            String ref = request.getHeader("Referer");
            if (!StringUtils.hasText(ref) && meta != null && meta.get("referrer") != null) {
                ref = String.valueOf(meta.get("referrer"));
            }
            event.setReferrer(TextSanitizer.plain(ref, 1000));
            event.setUserAgent(TextSanitizer.plain(request.getHeader("User-Agent"), 500));
            event.setIpHash(hashIp(clientIp(request)));
        } else if (meta != null && meta.get("referrer") != null) {
            event.setReferrer(TextSanitizer.plain(String.valueOf(meta.get("referrer")), 1000));
        }
        event.setMetaJson(toJson(meta));
        eventRepository.save(event);
    }

    private TrackingLinkEntity requireActiveLink(String code) {
        return resolveLink(code)
                .filter(TrackingLinkEntity::isActive)
                .orElseThrow(() -> new com.taino.common.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "LINK_NOT_FOUND",
                        "Ссылка не найдена"
                ));
    }

    Optional<TrackingLinkEntity> resolveLink(String code) {
        if (!StringUtils.hasText(code)) {
            return Optional.empty();
        }
        return linkRepository.findByCodeIgnoreCase(code.trim());
    }

    Optional<TrackingLinkEntity> resolveLinkFromStartParam(String startParam) {
        if (!StringUtils.hasText(startParam)) {
            return Optional.empty();
        }
        String p = startParam.trim();
        String lower = p.toLowerCase(Locale.ROOT);
        if (lower.startsWith("trk_")) {
            return resolveLink(p.substring(4));
        }
        if (lower.startsWith("t_")) {
            return resolveLink(p.substring(2));
        }
        return Optional.empty();
    }

    public String buildTelegramUrl(String code) {
        return buildTelegramUrl(code, null);
    }

    public String buildTelegramUrl(TrackingLinkEntity link) {
        return buildTelegramUrl(link.getCode(), link);
    }

    public String buildTelegramUrl(String code, TrackingLinkEntity link) {
        // Telegram startapp payload cannot carry free-form UTM query strings.
        // Attribution is by trk_CODE; UTM fields live on the tracking_links row.
        String username = properties.getTelegram().getBotUsername();
        if (!StringUtils.hasText(username)) {
            return buildMiniAppRedirect(code, link);
        }
        String bot = username.startsWith("@") ? username.substring(1) : username;
        String shortName = properties.getTelegram().getMiniAppShortName();
        String payload = "trk_" + code;
        if (StringUtils.hasText(shortName)) {
            String name = shortName.startsWith("/") ? shortName.substring(1) : shortName;
            return "https://t.me/" + bot + "/" + name + "?startapp=" + payload;
        }
        return "https://t.me/" + bot + "?startapp=" + payload;
    }

    public String buildTrackUrl(String code) {
        return buildTrackUrl(code, null);
    }

    public String buildTrackUrl(TrackingLinkEntity link) {
        return buildTrackUrl(link.getCode(), link);
    }

    public String buildTrackUrl(String code, TrackingLinkEntity link) {
        String base = properties.getPublicApiUrl();
        String path = StringUtils.hasText(base)
                ? base.replaceAll("/$", "") + "/api/track/r/" + code
                : "/api/track/r/" + code;
        return appendUtm(path, link);
    }

    public String buildMiniAppRedirect(String code) {
        return buildMiniAppRedirect(code, null);
    }

    public String buildMiniAppRedirect(String code, TrackingLinkEntity link) {
        String mini = properties.getMiniAppUrl();
        if (!StringUtils.hasText(mini)) {
            return buildTelegramUrl(code, link);
        }
        String sep = mini.contains("?") ? "&" : "?";
        String url = mini + sep + "tgWebAppStartParam=trk_" + code;
        return appendUtm(url, link);
    }

    private static String appendUtm(String url, TrackingLinkEntity link) {
        if (link == null) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        appendQuery(sb, "utm_source", link.getUtmSource());
        appendQuery(sb, "utm_medium", link.getUtmMedium());
        appendQuery(sb, "utm_campaign", link.getUtmCampaign());
        appendQuery(sb, "utm_content", link.getUtmContent());
        appendQuery(sb, "utm_term", link.getUtmTerm());
        return sb.toString();
    }

    private static void appendQuery(StringBuilder sb, String key, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        sb.append(sb.indexOf("?") >= 0 ? '&' : '?');
        sb.append(key).append('=').append(urlEncode(value.trim()));
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }

    private String toJson(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String hashIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig).substring(0, 32);
        } catch (Exception e) {
            return null;
        }
    }
}

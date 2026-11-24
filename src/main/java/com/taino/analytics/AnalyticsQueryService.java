package com.taino.analytics;

import com.taino.room.MemberStatus;
import com.taino.room.RoomMemberRepository;
import com.taino.room.RoomPhase;
import com.taino.room.RoomRepository;
import com.taino.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsQueryService {

    private final AnalyticsEventRepository eventRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;

    public AnalyticsQueryService(
            AnalyticsEventRepository eventRepository,
            UserRepository userRepository,
            RoomRepository roomRepository,
            RoomMemberRepository memberRepository
    ) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.OverviewResponse overview(Instant from, Instant to) {
        long clicks = eventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.LINK_CLICK, from, to);
        long uniqueClicks = eventRepository.countDistinctVisitors(AnalyticsEventType.LINK_CLICK, from, to);
        long opens = eventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.MINIAPP_OPEN, from, to);
        long uniqueOpens = eventRepository.countDistinctUsers(AnalyticsEventType.MINIAPP_OPEN, from, to);
        long regs = eventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.USER_REGISTER, from, to);
        long logins = eventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.USER_LOGIN, from, to);
        long roomsCreated = eventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.ROOM_CREATED, from, to);
        long joins = eventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.ROOM_JOINED, from, to);
        long totalUsers = userRepository.count();
        long participants = memberRepository.countByStatusAndParticipatesTrue(MemberStatus.ACTIVE);
        long roomsTotal = roomRepository.count();
        long roomsDrawn = roomRepository.countByPhaseNot(RoomPhase.REGISTRATION);

        return new AnalyticsDtos.OverviewResponse(
                from, to, clicks, uniqueClicks, opens, uniqueOpens, regs, logins,
                roomsCreated, joins, totalUsers, participants, roomsTotal, roomsDrawn
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.FunnelResponse funnel(Instant from, Instant to) {
        long clicks = eventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.LINK_CLICK, from, to);
        long opens = Math.max(
                eventRepository.countDistinctUsers(AnalyticsEventType.MINIAPP_OPEN, from, to),
                eventRepository.countDistinctVisitors(AnalyticsEventType.MINIAPP_OPEN, from, to)
        );
        long regs = eventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.USER_REGISTER, from, to);
        long creators = eventRepository.countDistinctUsers(AnalyticsEventType.ROOM_CREATED, from, to);
        long joiners = eventRepository.countDistinctUsers(AnalyticsEventType.ROOM_JOINED, from, to);
        long participants = memberRepository.countByStatusAndParticipatesTrue(MemberStatus.ACTIVE);
        long drawn = roomRepository.countByPhaseNot(RoomPhase.REGISTRATION);

        List<AnalyticsDtos.FunnelStep> steps = new ArrayList<>();
        long prev = 0;
        prev = addStep(steps, "clicks", "Клики по ссылкам", clicks, prev);
        prev = addStep(steps, "opens", "Открыли Mini App", opens, prev);
        prev = addStep(steps, "regs", "Регистрации", regs, prev);
        prev = addStep(steps, "joiners", "Вошли в комнату", joiners, prev);
        prev = addStep(steps, "creators", "Создали комнату", creators, prev);
        prev = addStep(steps, "participants", "Участвуют в раздачах", participants, prev);
        addStep(steps, "drawn", "Комнат с раздачей", drawn, prev);
        return new AnalyticsDtos.FunnelResponse(from, to, steps);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.TimeseriesResponse timeseries(Instant from, Instant to) {
        List<AnalyticsDtos.TimeseriesPoint> points = new ArrayList<>();
        for (Object[] row : eventRepository.timeseriesDaily(from, to)) {
            String day = toDay(row[0]);
            String type = String.valueOf(row[1]);
            long cnt = toLong(row[2]);
            points.add(new AnalyticsDtos.TimeseriesPoint(day, type, cnt));
        }
        return new AnalyticsDtos.TimeseriesResponse(from, to, points);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.SourcesResponse sources(Instant from, Instant to) {
        List<AnalyticsDtos.SourceRow> rows = new ArrayList<>();
        for (Object[] row : eventRepository.sourcesBreakdown(from, to)) {
            rows.add(new AnalyticsDtos.SourceRow(
                    String.valueOf(row[0]),
                    String.valueOf(row[1]),
                    String.valueOf(row[2]),
                    String.valueOf(row[3]),
                    String.valueOf(row[4]),
                    toLong(row[5]),
                    toLong(row[6]),
                    toLong(row[7])
            ));
        }
        List<AnalyticsDtos.ReferrerRow> referrers = new ArrayList<>();
        for (Object[] row : eventRepository.topReferrers(from, to)) {
            referrers.add(new AnalyticsDtos.ReferrerRow(String.valueOf(row[0]), toLong(row[1])));
        }
        return new AnalyticsDtos.SourcesResponse(from, to, rows, referrers);
    }

    private static long addStep(List<AnalyticsDtos.FunnelStep> steps, String id, String label, long value, long prev) {
        Double conv = null;
        if (prev > 0) {
            conv = Math.round(value * 1000.0 / prev) / 10.0;
        }
        steps.add(new AnalyticsDtos.FunnelStep(id, label, value, conv));
        return value;
    }

    private static String toDay(Object value) {
        if (value instanceof Date d) {
            return d.toLocalDate().toString();
        }
        if (value instanceof LocalDate ld) {
            return ld.toString();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneOffset.UTC).toLocalDate().toString();
        }
        return String.valueOf(value);
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof BigDecimal bd) return bd.longValue();
        return Long.parseLong(String.valueOf(value));
    }
}

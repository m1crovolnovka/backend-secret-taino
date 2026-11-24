package com.taino.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, Long> {

    long countByEventTypeAndCreatedAtBetween(String eventType, Instant from, Instant to);

    long countByEventTypeAndLinkIdAndCreatedAtBetween(String eventType, Long linkId, Instant from, Instant to);

    @Query("""
            select count(distinct e.visitorKey) from AnalyticsEventEntity e
            where e.eventType = :type and e.createdAt >= :from and e.createdAt < :to
              and e.visitorKey is not null
            """)
    long countDistinctVisitors(
            @Param("type") String type,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select count(distinct e.userId) from AnalyticsEventEntity e
            where e.eventType = :type and e.createdAt >= :from and e.createdAt < :to
              and e.userId is not null
            """)
    long countDistinctUsers(
            @Param("type") String type,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            select cast(date_trunc('day', e.created_at) as date) as day,
                   e.event_type as event_type,
                   count(*) as cnt
            from analytics_events e
            where e.created_at >= :from and e.created_at < :to
            group by 1, 2
            order by 1
            """, nativeQuery = true)
    List<Object[]> timeseriesDaily(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            select coalesce(l.utm_source, '(direct)') as source,
                   coalesce(l.utm_medium, '') as medium,
                   coalesce(l.utm_campaign, '') as campaign,
                   coalesce(l.code, '') as code,
                   coalesce(l.title, 'Без ссылки') as title,
                   count(*) filter (where e.event_type = 'LINK_CLICK') as clicks,
                   count(*) filter (where e.event_type = 'MINIAPP_OPEN') as opens,
                   count(distinct e.user_id) filter (where e.event_type in ('USER_REGISTER','USER_LOGIN')) as users
            from analytics_events e
            left join tracking_links l on l.id = e.link_id
            where e.created_at >= :from and e.created_at < :to
            group by 1, 2, 3, 4, 5
            order by clicks desc, opens desc
            """, nativeQuery = true)
    List<Object[]> sourcesBreakdown(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            select coalesce(nullif(trim(e.referrer), ''), '(empty)') as referrer,
                   count(*) as cnt
            from analytics_events e
            where e.event_type = 'LINK_CLICK'
              and e.created_at >= :from and e.created_at < :to
            group by 1
            order by cnt desc
            limit 30
            """, nativeQuery = true)
    List<Object[]> topReferrers(@Param("from") Instant from, @Param("to") Instant to);
}

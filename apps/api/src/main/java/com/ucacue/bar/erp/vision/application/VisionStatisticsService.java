package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionDetectionEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionEventEntity;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionDetectionReportRow;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionRecentEventResponse;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisionStatisticsService {

    private final JdbcTemplate jdbcTemplate;

    public void updateForEvent(VisionEventEntity event, List<VisionDetectionEntity> detections) {
        if (detections == null || detections.isEmpty()) {
            return;
        }

        Map<String, MutableLabelStats> byLabel = new LinkedHashMap<>();
        for (VisionDetectionEntity detection : detections) {
            byLabel.computeIfAbsent(detection.getLabel(), MutableLabelStats::new).add(detection.getConfidence());
        }

        OffsetDateTime eventTime = event.getEventTimestamp();
        OffsetDateTime hourStart = eventTime.truncatedTo(ChronoUnit.HOURS);
        OffsetDateTime hourEnd = hourStart.plusHours(1);
        LocalDate day = eventTime.toLocalDate();
        LocalDate month = LocalDate.of(day.getYear(), day.getMonth(), 1);

        for (MutableLabelStats mutableStats : byLabel.values()) {
            LabelStats stats = mutableStats.immutable();
            upsertHourly(event, stats, hourStart, hourEnd);
            upsertDaily(event, stats, day);
            upsertMonthly(event, stats, month);
        }
    }

    public VisionStatisticsResponse statistics(Long tenantId,
                                               LocalDate from,
                                               LocalDate to,
                                               String requestedPeriod,
                                               Long cameraId) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(7);
        LocalDate endDate = to != null ? to : LocalDate.now();
        Period period = Period.from(requestedPeriod);
        Range range = Range.of(startDate, endDate, period);

        long totalEvents = totalEvents(tenantId, range, cameraId);
        long totalDetections = totalDetections(period, tenantId, range, cameraId);
        long activeCameras = activeCameras(tenantId, cameraId);
        String topLabel = topLabel(period, tenantId, range, cameraId);

        return new VisionStatisticsResponse(
                period.apiName,
                startDate.toString(),
                endDate.toString(),
                new VisionStatisticsResponse.Summary(totalEvents, totalDetections, activeCameras, topLabel),
                byLabel(period, tenantId, range, cameraId),
                byCamera(period, tenantId, range, cameraId),
                timeline(period, tenantId, range, cameraId));
    }

    public List<VisionRecentEventResponse> recentEvents(Long tenantId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jdbcTemplate.query("""
                        select e.id,
                               e.event_timestamp,
                               e.frame_id,
                               e.total_detections,
                               e.status,
                               e.received_at,
                               c.code as camera_code,
                               c.name as camera_name,
                               b.code as location_code
                        from vision_events e
                        join vision_cameras c on c.id = e.camera_id and c.tenant_id = e.tenant_id
                        left join erp_branches b on b.id = e.branch_id and b.tenant_id = e.tenant_id
                        where e.tenant_id = ?
                        order by e.event_timestamp desc, e.id desc
                        limit ?
                        """,
                (rs, rowNum) -> new VisionRecentEventResponse(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("camera_code"),
                        rs.getString("camera_name"),
                        rs.getString("location_code"),
                        toOffset(rs.getTimestamp("event_timestamp")),
                        rs.getString("frame_id"),
                        rs.getInt("total_detections"),
                        rs.getString("status"),
                        toOffset(rs.getTimestamp("received_at"))),
                tenantId,
                safeLimit);
    }

    public List<VisionDetectionReportRow> detectionReport(Long tenantId,
                                                          LocalDate from,
                                                          LocalDate to,
                                                          int limit) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now();
        Range range = Range.of(start, end, Period.DAILY);
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return jdbcTemplate.query("""
                        select e.id as event_id,
                               e.event_timestamp,
                               c.code as camera_code,
                               c.name as camera_name,
                               b.code as location_code,
                               d.label,
                               d.confidence,
                               d.bbox_x,
                               d.bbox_y,
                               d.bbox_width,
                               d.bbox_height,
                               d.tracking_id
                        from vision_detections d
                        join vision_events e on e.id = d.event_id and e.tenant_id = d.tenant_id
                        join vision_cameras c on c.id = d.camera_id and c.tenant_id = d.tenant_id
                        left join erp_branches b on b.id = d.branch_id and b.tenant_id = d.tenant_id
                        where d.tenant_id = ?
                          and e.event_timestamp >= ?
                          and e.event_timestamp < ?
                        order by e.event_timestamp desc, d.id desc
                        limit ?
                        """,
                (rs, rowNum) -> new VisionDetectionReportRow(
                        String.valueOf(rs.getLong("event_id")),
                        toOffset(rs.getTimestamp("event_timestamp")),
                        rs.getString("camera_code"),
                        rs.getString("camera_name"),
                        rs.getString("location_code"),
                        rs.getString("label"),
                        rs.getBigDecimal("confidence"),
                        rs.getBigDecimal("bbox_x"),
                        rs.getBigDecimal("bbox_y"),
                        rs.getBigDecimal("bbox_width"),
                        rs.getBigDecimal("bbox_height"),
                        rs.getString("tracking_id")),
                tenantId,
                ts(range.start()),
                ts(range.endExclusive()),
                safeLimit);
    }

    private void upsertHourly(VisionEventEntity event,
                              LabelStats stats,
                              OffsetDateTime periodStart,
                              OffsetDateTime periodEnd) {
        jdbcTemplate.update("""
                        insert into vision_statistics_hourly
                            (tenant_id, camera_id, branch_id, label, period_start, period_end, total_events,
                             total_detections, confidence_sum, avg_confidence, max_confidence, min_confidence)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on conflict (tenant_id, camera_id, branch_id, label, period_start)
                        do update set
                            total_events = vision_statistics_hourly.total_events + excluded.total_events,
                            total_detections = vision_statistics_hourly.total_detections + excluded.total_detections,
                            confidence_sum = vision_statistics_hourly.confidence_sum + excluded.confidence_sum,
                            avg_confidence = (vision_statistics_hourly.confidence_sum + excluded.confidence_sum)
                                / nullif((vision_statistics_hourly.total_detections + excluded.total_detections), 0),
                            max_confidence = greatest(coalesce(vision_statistics_hourly.max_confidence, excluded.max_confidence), excluded.max_confidence),
                            min_confidence = least(coalesce(vision_statistics_hourly.min_confidence, excluded.min_confidence), excluded.min_confidence),
                            updated_at = current_timestamp
                        """,
                event.getTenantId(),
                event.getCameraId(),
                event.getBranchId(),
                stats.label(),
                ts(periodStart),
                ts(periodEnd),
                1,
                stats.count(),
                stats.sum(),
                stats.average(),
                stats.max(),
                stats.min());
    }

    private void upsertDaily(VisionEventEntity event, LabelStats stats, LocalDate periodDate) {
        jdbcTemplate.update("""
                        insert into vision_statistics_daily
                            (tenant_id, camera_id, branch_id, label, period_date, total_events,
                             total_detections, confidence_sum, avg_confidence, max_confidence, min_confidence)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on conflict (tenant_id, camera_id, branch_id, label, period_date)
                        do update set
                            total_events = vision_statistics_daily.total_events + excluded.total_events,
                            total_detections = vision_statistics_daily.total_detections + excluded.total_detections,
                            confidence_sum = vision_statistics_daily.confidence_sum + excluded.confidence_sum,
                            avg_confidence = (vision_statistics_daily.confidence_sum + excluded.confidence_sum)
                                / nullif((vision_statistics_daily.total_detections + excluded.total_detections), 0),
                            max_confidence = greatest(coalesce(vision_statistics_daily.max_confidence, excluded.max_confidence), excluded.max_confidence),
                            min_confidence = least(coalesce(vision_statistics_daily.min_confidence, excluded.min_confidence), excluded.min_confidence),
                            updated_at = current_timestamp
                        """,
                event.getTenantId(),
                event.getCameraId(),
                event.getBranchId(),
                stats.label(),
                Date.valueOf(periodDate),
                1,
                stats.count(),
                stats.sum(),
                stats.average(),
                stats.max(),
                stats.min());
    }

    private void upsertMonthly(VisionEventEntity event, LabelStats stats, LocalDate periodMonth) {
        jdbcTemplate.update("""
                        insert into vision_statistics_monthly
                            (tenant_id, camera_id, branch_id, label, period_month, total_events,
                             total_detections, confidence_sum, avg_confidence, max_confidence, min_confidence)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on conflict (tenant_id, camera_id, branch_id, label, period_month)
                        do update set
                            total_events = vision_statistics_monthly.total_events + excluded.total_events,
                            total_detections = vision_statistics_monthly.total_detections + excluded.total_detections,
                            confidence_sum = vision_statistics_monthly.confidence_sum + excluded.confidence_sum,
                            avg_confidence = (vision_statistics_monthly.confidence_sum + excluded.confidence_sum)
                                / nullif((vision_statistics_monthly.total_detections + excluded.total_detections), 0),
                            max_confidence = greatest(coalesce(vision_statistics_monthly.max_confidence, excluded.max_confidence), excluded.max_confidence),
                            min_confidence = least(coalesce(vision_statistics_monthly.min_confidence, excluded.min_confidence), excluded.min_confidence),
                            updated_at = current_timestamp
                        """,
                event.getTenantId(),
                event.getCameraId(),
                event.getBranchId(),
                stats.label(),
                Date.valueOf(periodMonth),
                1,
                stats.count(),
                stats.sum(),
                stats.average(),
                stats.max(),
                stats.min());
    }

    private long totalEvents(Long tenantId, Range range, Long cameraId) {
        if (cameraId == null) {
            return longValue(jdbcTemplate.queryForObject("""
                            select count(*)
                            from vision_events
                            where tenant_id = ?
                              and event_timestamp >= ?
                              and event_timestamp < ?
                            """,
                    Long.class,
                    tenantId,
                    ts(range.start()),
                    ts(range.endExclusive())));
        }
        return longValue(jdbcTemplate.queryForObject("""
                        select count(*)
                        from vision_events
                        where tenant_id = ?
                          and camera_id = ?
                          and event_timestamp >= ?
                          and event_timestamp < ?
                        """,
                Long.class,
                tenantId,
                cameraId,
                ts(range.start()),
                ts(range.endExclusive())));
    }

    private long totalDetections(Period period, Long tenantId, Range range, Long cameraId) {
        String cameraFilter = cameraId == null ? "" : " and camera_id = ? ";
        List<Object> args = period.args(tenantId, range, cameraId);
        return longValue(jdbcTemplate.queryForObject("""
                        select coalesce(sum(total_detections), 0)
                        from %s
                        where tenant_id = ?
                          and %s
                        %s
                        """.formatted(period.table, period.rangePredicate, cameraFilter),
                Long.class,
                args.toArray()));
    }

    private long activeCameras(Long tenantId, Long cameraId) {
        if (cameraId == null) {
            return longValue(jdbcTemplate.queryForObject("""
                            select count(*)
                            from vision_cameras
                            where tenant_id = ?
                              and status = 'ACTIVE'
                            """,
                    Long.class,
                    tenantId));
        }
        return longValue(jdbcTemplate.queryForObject("""
                        select count(*)
                        from vision_cameras
                        where tenant_id = ?
                          and id = ?
                          and status = 'ACTIVE'
                        """,
                Long.class,
                tenantId,
                cameraId));
    }

    private String topLabel(Period period, Long tenantId, Range range, Long cameraId) {
        String cameraFilter = cameraId == null ? "" : " and camera_id = ? ";
        List<Object> args = period.args(tenantId, range, cameraId);
        return jdbcTemplate.query("""
                        select label
                        from %s
                        where tenant_id = ?
                          and %s
                        %s
                        group by label
                        order by sum(total_detections) desc, label asc
                        limit 1
                        """.formatted(period.table, period.rangePredicate, cameraFilter),
                rs -> rs.next() ? rs.getString("label") : null,
                args.toArray());
    }

    private List<VisionStatisticsResponse.LabelTotal> byLabel(Period period,
                                                              Long tenantId,
                                                              Range range,
                                                              Long cameraId) {
        String cameraFilter = cameraId == null ? "" : " and camera_id = ? ";
        List<Object> args = period.args(tenantId, range, cameraId);
        return jdbcTemplate.query("""
                        select label, coalesce(sum(total_detections), 0) as total
                        from %s
                        where tenant_id = ?
                          and %s
                        %s
                        group by label
                        order by total desc, label asc
                        """.formatted(period.table, period.rangePredicate, cameraFilter),
                (rs, rowNum) -> new VisionStatisticsResponse.LabelTotal(
                        rs.getString("label"),
                        rs.getLong("total")),
                args.toArray());
    }

    private List<VisionStatisticsResponse.CameraTotal> byCamera(Period period,
                                                                Long tenantId,
                                                                Range range,
                                                                Long cameraId) {
        String cameraFilter = cameraId == null ? "" : " and s.camera_id = ? ";
        List<Object> args = period.argsWithAlias("s", tenantId, range, cameraId);
        return jdbcTemplate.query("""
                        select c.code, c.name, coalesce(sum(s.total_detections), 0) as total
                        from %s s
                        join vision_cameras c on c.id = s.camera_id and c.tenant_id = s.tenant_id
                        where s.tenant_id = ?
                          and %s
                        %s
                        group by c.code, c.name
                        order by total desc, c.code asc
                        """.formatted(period.table, period.aliasedRangePredicate("s"), cameraFilter),
                (rs, rowNum) -> new VisionStatisticsResponse.CameraTotal(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getLong("total")),
                args.toArray());
    }

    private List<VisionStatisticsResponse.TimelinePoint> timeline(Period period,
                                                                  Long tenantId,
                                                                  Range range,
                                                                  Long cameraId) {
        String cameraFilter = cameraId == null ? "" : " and camera_id = ? ";
        List<Object> args = period.args(tenantId, range, cameraId);
        return jdbcTemplate.query("""
                        select %s as bucket, coalesce(sum(total_detections), 0) as total
                        from %s
                        where tenant_id = ?
                          and %s
                        %s
                        group by %s
                        order by %s asc
                        """.formatted(period.bucketColumn, period.table, period.rangePredicate,
                cameraFilter, period.bucketColumn, period.bucketColumn),
                (rs, rowNum) -> new VisionStatisticsResponse.TimelinePoint(
                        period.toOffset(rs.getObject("bucket")),
                        rs.getLong("total")),
                args.toArray());
    }

    private Timestamp ts(OffsetDateTime value) {
        return Timestamp.from(value.toInstant());
    }

    private OffsetDateTime toOffset(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private long longValue(Long value) {
        return value == null ? 0L : value;
    }

    private record LabelStats(
            String label,
            int count,
            BigDecimal sum,
            BigDecimal min,
            BigDecimal max) {

        private BigDecimal average() {
            return count == 0
                    ? BigDecimal.ZERO
                    : sum.divide(BigDecimal.valueOf(count), 5, java.math.RoundingMode.HALF_UP);
        }
    }

    private static final class MutableLabelStats {
        private final String label;
        private int count;
        private BigDecimal sum = BigDecimal.ZERO;
        private BigDecimal min;
        private BigDecimal max;

        private MutableLabelStats(String label) {
            this.label = label;
        }

        private void add(BigDecimal confidence) {
            count++;
            sum = sum.add(confidence);
            min = min == null || confidence.compareTo(min) < 0 ? confidence : min;
            max = max == null || confidence.compareTo(max) > 0 ? confidence : max;
        }

        private LabelStats immutable() {
            return new LabelStats(label, count, sum, min, max);
        }
    }

    private record Range(
            OffsetDateTime start,
            OffsetDateTime endExclusive,
            LocalDate startDate,
            LocalDate endDate) {

        private static Range of(LocalDate from, LocalDate to, Period period) {
            LocalDate safeTo = to.isBefore(from) ? from : to;
            LocalDate startDate = period == Period.MONTHLY
                    ? LocalDate.of(from.getYear(), from.getMonth(), 1)
                    : from;
            LocalDate endDate = period == Period.MONTHLY
                    ? LocalDate.of(safeTo.getYear(), safeTo.getMonth(), 1)
                    : safeTo;
            return new Range(
                    from.atStartOfDay().atOffset(ZoneOffset.UTC),
                    safeTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC),
                    startDate,
                    endDate);
        }
    }

    private enum Period {
        HOURLY("hourly", "vision_statistics_hourly", "period_start", "period_start >= ? and period_start < ?"),
        DAILY("daily", "vision_statistics_daily", "period_date", "period_date >= ? and period_date <= ?"),
        MONTHLY("monthly", "vision_statistics_monthly", "period_month", "period_month >= ? and period_month <= ?");

        private final String apiName;
        private final String table;
        private final String bucketColumn;
        private final String rangePredicate;

        Period(String apiName, String table, String bucketColumn, String rangePredicate) {
            this.apiName = apiName;
            this.table = table;
            this.bucketColumn = bucketColumn;
            this.rangePredicate = rangePredicate;
        }

        private static Period from(String value) {
            if (value == null || value.isBlank()) {
                return DAILY;
            }
            return switch (value.trim().toLowerCase()) {
                case "hourly", "hour" -> HOURLY;
                case "monthly", "month" -> MONTHLY;
                default -> DAILY;
            };
        }

        private List<Object> args(Long tenantId, Range range, Long cameraId) {
            List<Object> args = new java.util.ArrayList<>();
            args.add(tenantId);
            if (this == HOURLY) {
                args.add(Timestamp.from(range.start().toInstant()));
                args.add(Timestamp.from(range.endExclusive().toInstant()));
            } else {
                args.add(Date.valueOf(range.startDate()));
                args.add(Date.valueOf(range.endDate()));
            }
            if (cameraId != null) {
                args.add(cameraId);
            }
            return args;
        }

        private List<Object> argsWithAlias(String alias, Long tenantId, Range range, Long cameraId) {
            return args(tenantId, range, cameraId);
        }

        private String aliasedRangePredicate(String alias) {
            return rangePredicate.replace(bucketColumn, alias + "." + bucketColumn);
        }

        private OffsetDateTime toOffset(Object value) {
            if (value instanceof Timestamp timestamp) {
                return timestamp.toInstant().atOffset(ZoneOffset.UTC);
            }
            if (value instanceof Date date) {
                return date.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            if (value instanceof LocalDate localDate) {
                return localDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            return null;
        }
    }
}

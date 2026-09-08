package fr.pace.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SortieReadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SortieReadService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SortieReadService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SortieResponse> recent(int limit) {
        return jdbc.query("""
                        SELECT a.id,a.source,a.details_json
                        FROM activity a
                        LEFT JOIN activity_fit_summary summary ON summary.activity_id=a.id
                        ORDER BY COALESCE(summary.started_at,
                          NULLIF(a.details_json::jsonb ->> 'startedAt','')::timestamptz) DESC NULLS LAST,
                          a.discovered_at DESC
                        LIMIT ?
                        """,
                        (result, row) -> new StoredActivity(
                                result.getObject("id", java.util.UUID.class),
                                result.getString("source"),
                                result.getString("details_json")
                        ), limit)
                .stream()
                .map(this::toResponse).toList();
    }

    private SortieResponse toResponse(StoredActivity activity) {
        try {
            JsonNode details = objectMapper.readTree(activity.detailsJson());
            JsonNode metrics = details.path("availableMetrics");
            JsonNode summary = metrics.path("summaryDTO");
            return new SortieResponse(
                    activity.id(),
                    instant(details.path("startedAt")),
                    text(details.path("sport")),
                    longValue(summary, metrics, "distance"),
                    longValue(summary, metrics, "duration"),
                    activity.source()
            );
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Une sortie persistée contient des détails JSON illisibles", exception);
            return new SortieResponse(activity.id(), null, null, null, null, activity.source());
        }
    }

    private static Instant instant(JsonNode value) {
        if (!value.isTextual()) return null;
        try {
            return Instant.parse(value.textValue());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String text(JsonNode value) {
        return value.isTextual() ? value.textValue() : null;
    }

    private static Long longValue(JsonNode first, JsonNode second, String field) {
        JsonNode value = first.path(field);
        if (!value.isNumber()) value = second.path(field);
        return value.isNumber() ? value.longValue() : null;
    }
    private record StoredActivity(java.util.UUID id, String source, String detailsJson) { }
}

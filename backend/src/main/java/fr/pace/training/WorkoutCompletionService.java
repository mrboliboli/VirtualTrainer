package fr.pace.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.garmin.GarminSynchronizationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkoutCompletionService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final GarminSynchronizationProperties properties;
    private final PlannedWorkoutMatcher matcher;

    public WorkoutCompletionService(JdbcTemplate jdbc, ObjectMapper mapper, GarminSynchronizationProperties properties,
                                    PlannedWorkoutMatcher matcher) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.properties = properties;
        this.matcher = matcher;
    }

    @Transactional
    public void matchImportedActivity(UUID activityId) {
        Activity activity = jdbc.query("SELECT details_json FROM activity WHERE id=?",
                (rs, row) -> parse(activityId, rs.getString("details_json")), activityId).stream().findFirst().orElse(null);
        if (activity == null || activity.startedAt() == null) return;
        LocalDate activityDate = LocalDate.ofInstant(activity.startedAt(), properties.zoneId());
        List<PlannedWorkoutMatcher.Candidate> candidates = jdbc.query(
                "SELECT id,planned_date FROM planned_workout WHERE status='PLANIFIEE' " +
                        "AND completed_activity_id IS NULL AND planned_date BETWEEN ? AND ?",
                (rs, row) -> new PlannedWorkoutMatcher.Candidate(rs.getObject("id", UUID.class),
                        rs.getObject("planned_date", LocalDate.class)), activityDate.minusDays(1), activityDate.plusDays(1));
        matcher.match(activityDate, activity.sport(), candidates).ifPresent(candidate -> jdbc.update(
                "UPDATE planned_workout SET completed_activity_id=?,completion_matched_at=?," +
                        "completion_match_method='DATE_SPORT_FENETRE_1J',status='REALISEE',updated_at=? " +
                        "WHERE id=? AND completed_activity_id IS NULL AND status='PLANIFIEE'",
                activityId, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), candidate.id()));
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutCompletionResponse> find(UUID workoutId) {
        return jdbc.query("SELECT p.id,p.planned_date,p.status,p.completion_match_method,p.completion_matched_at," +
                        "a.id activity_id,s.started_at,s.sport,s.distance_meters,s.elapsed_seconds " +
                        "FROM planned_workout p LEFT JOIN activity a ON a.id=p.completed_activity_id " +
                        "LEFT JOIN activity_fit_summary s ON s.activity_id=a.id WHERE p.id=?",
                (rs, row) -> new WorkoutCompletionResponse(rs.getObject("id", UUID.class),
                        rs.getObject("planned_date", LocalDate.class), rs.getString("status"),
                        rs.getObject("activity_id", UUID.class), instant(rs, "started_at"), rs.getString("sport"),
                        number(rs, "distance_meters"), number(rs, "elapsed_seconds"),
                        rs.getString("completion_match_method"), instant(rs, "completion_matched_at")), workoutId)
                .stream().findFirst();
    }

    private Activity parse(UUID id, String json) {
        try {
            JsonNode details = mapper.readTree(json);
            return new Activity(id, Instant.parse(details.path("startedAt").asText()), details.path("sport").asText(null));
        } catch (RuntimeException | java.io.IOException ignored) {
            return new Activity(id, null, null);
        }
    }

    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
    private static Double number(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.doubleValue();
    }
    private record Activity(UUID id, Instant startedAt, String sport) { }
}

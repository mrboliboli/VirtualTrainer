package fr.pace.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.garmin.GarminSynchronizationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutCompletionServiceTest {
    @Test
    void persistsTheLinkAndExposesCompletionData() {
        var database = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(database);
            jdbc.execute("CREATE TABLE activity(id UUID PRIMARY KEY,details_json CLOB)");
            jdbc.execute("CREATE TABLE activity_fit_summary(activity_id UUID PRIMARY KEY,started_at TIMESTAMP WITH TIME ZONE," +
                    "sport VARCHAR(50),distance_meters DOUBLE,elapsed_seconds DOUBLE)");
            jdbc.execute("CREATE TABLE planned_workout(id UUID PRIMARY KEY,planned_date DATE,status VARCHAR(20)," +
                    "completed_activity_id UUID,completion_matched_at TIMESTAMP WITH TIME ZONE," +
                    "completion_match_method VARCHAR(40),updated_at TIMESTAMP WITH TIME ZONE)");
            UUID activityId = UUID.randomUUID();
            UUID workoutId = UUID.randomUUID();
            jdbc.update("INSERT INTO activity VALUES (?,?)", activityId,
                    "{\"startedAt\":\"2026-09-10T06:00:00Z\",\"sport\":\"running\"}");
            jdbc.update("INSERT INTO activity_fit_summary VALUES (?,?,?,?,?)", activityId,
                    java.sql.Timestamp.from(java.time.Instant.parse("2026-09-10T06:00:00Z")), "running", 8000d, 2800d);
            jdbc.update("INSERT INTO planned_workout(id,planned_date,status) VALUES (?,?,?)", workoutId,
                    LocalDate.of(2026, 9, 10), "PLANIFIEE");
            var service = new WorkoutCompletionService(jdbc, new ObjectMapper(),
                    new GarminSynchronizationProperties(null, 2, 7, ZoneId.of("Europe/Paris")),
                    new PlannedWorkoutMatcher());

            service.matchImportedActivity(activityId);

            WorkoutCompletionResponse result = service.find(workoutId).orElseThrow();
            assertThat(result.statut()).isEqualTo("REALISEE");
            assertThat(result.activiteId()).isEqualTo(activityId);
            assertThat(result.distanceMetres()).isEqualTo(8000d);
            assertThat(result.methodeRapprochement()).isEqualTo("DATE_SPORT_FENETRE_1J");
        } finally {
            database.shutdown();
        }
    }
}

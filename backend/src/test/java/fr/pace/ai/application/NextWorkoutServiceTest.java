package fr.pace.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.SortieReadService;
import fr.pace.activity.SortieResponse;
import fr.pace.ai.domain.AiProvider;
import fr.pace.goal.GoalService;
import fr.pace.profile.AthleteProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NextWorkoutServiceTest {
    private JdbcTemplate jdbc;
    private NextWorkoutService service;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:workouts-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE planned_workout(
                  id UUID PRIMARY KEY, title VARCHAR(200), planned_date DATE, duration_minutes INTEGER,
                  intensity VARCHAR(30), steps_json VARCHAR(4000), rationale VARCHAR(1500), confidence VARCHAR(20),
                  status VARCHAR(20), created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE,
                  decided_at TIMESTAMP WITH TIME ZONE, version INTEGER, previous_workout_id UUID)
                """);
        jdbc.execute("CREATE TABLE ai_call(activity_id UUID,operation_type VARCHAR(30),status VARCHAR(30)," +
                "analysis_version INTEGER,response_json CLOB)");
        service = new NextWorkoutService(jdbc, new ObjectMapper().findAndRegisterModules(), mock(AiProvider.class),
                mock(AiSettingsService.class), mock(GoalService.class), mock(AthleteProfileService.class), mock(SortieReadService.class));
    }

    @Test
    void acceptsAProposalAndKeepsItsVersionInHistory() {
        UUID id = insert("PROPOSEE", 2, null);

        var accepted = service.accept(id);

        assertThat(accepted.statut()).isEqualTo("PLANIFIEE");
        assertThat(accepted.version()).isEqualTo(2);
        assertThat(service.history()).extracting("id").containsExactly(id);
        assertThat(jdbc.queryForObject("SELECT decided_at IS NOT NULL FROM planned_workout WHERE id=?", Boolean.class, id)).isTrue();
    }

    @Test
    void refusesAProposalButDoesNotDeleteIt() {
        UUID id = insert("PROPOSEE", 1, null);

        assertThat(service.refuse(id).statut()).isEqualTo("REFUSEE");
        assertThat(service.history()).hasSize(1);
        assertThatThrownBy(() -> service.accept(id)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("proposée");
    }

    @Test
    void selectsTheNextConfiguredAvailableDay() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        assertThat(NextWorkoutService.split("Mercredi|Vendredi|Dimanche"))
                .containsExactly("Mercredi", "Vendredi", "Dimanche");
        assertThat(NextWorkoutService.nextAvailableDate(monday, List.of("Mercredi", "samedi")))
                .isEqualTo(LocalDate.of(2026, 9, 9));
    }

    @Test
    void rejectsPastDatesExcessDurationAndUnavailableDays() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        assertThatThrownBy(() -> NextWorkoutService.validate(monday, 30, monday, List.of(), 60))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("futur");
        assertThatThrownBy(() -> NextWorkoutService.validate(monday.plusDays(1), 61, monday, List.of(), 60))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("durée maximale");
        assertThatThrownBy(() -> NextWorkoutService.validate(monday.plusDays(1), 30, monday, List.of("mercredi"), 60))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("jours disponibles");
    }

    @Test
    void enrichesRecentActivityWithItsLatestSuccessfulAnalysis() {
        UUID activityId = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_call VALUES (?,?,?,?,?)", activityId, "ACTIVITY_ANALYSIS", "REUSSIE", 1, """
                {"summary":"Effort régulier","recovery":{"recommendation":"Footing léger"},
                 "nextWorkoutImpact":"Éviter le seuil"}
                """);
        var activity = new SortieResponse(activityId, Instant.parse("2026-09-08T08:00:00Z"), "running",
                10_000L, 3_600L, 145, "Base", "GARMIN_PERSONNEL");

        assertThat(service.activitySummary(activity))
                .contains("Effort régulier", "Footing léger", "Éviter le seuil");
    }

    private UUID insert(String status, int version, UUID previousId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO planned_workout(id,title,planned_date,duration_minutes,intensity,steps_json,rationale,
                  confidence,status,created_at,updated_at,version,previous_workout_id)
                VALUES (?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?)
                """, id, "Footing", LocalDate.now().plusDays(2), 30, "FACILE", "[\"30 min faciles\"]",
                "Récupération", "MOYEN", status, version, previousId);
        return id;
    }
}

package fr.pace.ai.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.SortieReadService;
import fr.pace.ai.api.NextWorkoutResponse;
import fr.pace.ai.domain.AiProvider;
import fr.pace.ai.domain.WorkoutGenerationRequest;
import fr.pace.goal.Goal;
import fr.pace.goal.GoalService;
import fr.pace.profile.AthleteProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class NextWorkoutService {
    private final JdbcTemplate jdbc; private final ObjectMapper mapper; private final AiProvider provider;
    private final AiSettingsService settings; private final GoalService goals; private final AthleteProfileService profiles;
    private final SortieReadService activities; private final Clock clock = Clock.systemDefaultZone();

    public NextWorkoutService(JdbcTemplate jdbc, ObjectMapper mapper, AiProvider provider, AiSettingsService settings,
                              GoalService goals, AthleteProfileService profiles, SortieReadService activities) {
        this.jdbc = jdbc; this.mapper = mapper; this.provider = provider; this.settings = settings;
        this.goals = goals; this.profiles = profiles; this.activities = activities;
    }

    @Transactional(readOnly = true)
    public NextWorkoutResponse next() {
        return jdbc.query("SELECT * FROM planned_workout WHERE status='PLANIFIEE' AND planned_date>=CURRENT_DATE ORDER BY planned_date,created_at DESC LIMIT 1",
                (rs, row) -> map(rs)).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune prochaine séance."));
    }

    @Transactional
    public NextWorkoutResponse generate() {
        var configuration = settings.getOrCreate();
        if (!configuration.isEnabled()) throw new ResponseStatusException(HttpStatus.CONFLICT, "L'IA est désactivée.");
        if (!settings.keyConfigured()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La clé OpenAI n'est pas configurée.");
        LocalDate today = LocalDate.now(clock);
        Goal goal = goals.list().stream().filter(g -> !g.getEventDate().isBefore(today))
                .sorted(Comparator.comparing(Goal::isPrimary).reversed().thenComparing(Goal::getEventDate)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Définis d'abord un objectif futur."));
        var profile = profiles.get();
        LocalDate target = today.plusDays(1);
        List<String> summaries = activities.recent(3).stream().map(a -> "%s : %s, %s m, %s s".formatted(
                a.dateHeure(), a.sport(), a.distanceMetres(), a.dureeSecondes())).toList();
        String goalSummary = "%s le %s, distance %s %s".formatted(goal.getName(), goal.getEventDate(), goal.getDistance(), goal.getDistanceUnit());
        List<String> constraints = profile.getConstraintsAndInjuries() == null ? List.of() : List.of(profile.getConstraintsAndInjuries());
        List<String> days = split(profile.getAvailableDays());
        var result = provider.generateWorkout(new WorkoutGenerationRequest(target, goalSummary, summaries, constraints,
                days, profile.getMaximumSessionDurationMinutes()));
        UUID id = UUID.randomUUID(); Instant now = clock.instant();
        Timestamp databaseNow = Timestamp.from(now);
        jdbc.update("UPDATE planned_workout SET status='REMPLACEE',updated_at=? WHERE status='PLANIFIEE'", databaseNow);
        try {
            jdbc.update("INSERT INTO planned_workout(id,goal_id,title,planned_date,objective,duration_minutes,intensity,steps_json,rationale,confidence,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, goal.getId(), result.title(), result.plannedDate(), result.objective(), result.durationMinutes(),
                    result.intensity(), mapper.writeValueAsString(result.steps()), result.rationale(), result.confidence().name(),
                    "PLANIFIEE", databaseNow, databaseNow);
        } catch (Exception exception) { throw new IllegalStateException("La séance générée ne peut pas être enregistrée.", exception); }
        return next();
    }

    public boolean automaticEnabled() { return settings.getOrCreate().isAutoGenerateWorkoutAfterImport(); }

    private NextWorkoutResponse map(java.sql.ResultSet rs) throws java.sql.SQLException {
        try {
            return new NextWorkoutResponse(rs.getObject("id", UUID.class), rs.getString("title"),
                    rs.getObject("planned_date", LocalDate.class), "COURSE", rs.getInt("duration_minutes"), null,
                    rs.getString("intensity"), rs.getString("rationale"),
                    mapper.readValue(rs.getString("steps_json"), new TypeReference<>() {}), rs.getString("confidence"), rs.getString("status"));
        } catch (java.io.IOException exception) { throw new java.sql.SQLException(exception); }
    }
    private static List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split("[,;]"))
                .map(String::trim).filter(v -> !v.isBlank()).toList();
    }
}

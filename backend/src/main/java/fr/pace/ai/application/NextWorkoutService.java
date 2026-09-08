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
import java.text.Normalizer;

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
        return jdbc.query("SELECT * FROM planned_workout WHERE status IN ('PLANIFIEE','PROPOSEE') AND planned_date>=CURRENT_DATE " +
                        "ORDER BY CASE status WHEN 'PROPOSEE' THEN 0 ELSE 1 END, planned_date,created_at DESC LIMIT 1",
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
        List<String> days = split(profile.getAvailableDays());
        LocalDate target = nextAvailableDate(today, days);
        List<String> summaries = activities.recent(3).stream().map(a -> "%s : %s, %s m, %s s".formatted(
                a.dateHeure(), a.sport(), a.distanceMetres(), a.dureeSecondes())).toList();
        String goalSummary = "%s le %s, distance %s %s".formatted(goal.getName(), goal.getEventDate(), goal.getDistance(), goal.getDistanceUnit());
        List<String> constraints = profile.getConstraintsAndInjuries() == null ? List.of() : List.of(profile.getConstraintsAndInjuries());
        var result = provider.generateWorkout(new WorkoutGenerationRequest(target, goalSummary, summaries, constraints,
                days, profile.getMaximumSessionDurationMinutes()));
        validate(result.plannedDate(), result.durationMinutes(), today, days, profile.getMaximumSessionDurationMinutes());
        UUID id = UUID.randomUUID(); Instant now = clock.instant();
        Timestamp databaseNow = Timestamp.from(now);
        UUID previousId = jdbc.query("SELECT id FROM planned_workout WHERE status='PROPOSEE' ORDER BY created_at DESC LIMIT 1",
                (rs, row) -> rs.getObject("id", UUID.class)).stream().findFirst().orElse(null);
        int version = Optional.ofNullable(jdbc.queryForObject("SELECT COALESCE(MAX(version),0)+1 FROM planned_workout", Integer.class)).orElse(1);
        jdbc.update("UPDATE planned_workout SET status='REMPLACEE',updated_at=? WHERE status='PROPOSEE'", databaseNow);
        try {
            jdbc.update("INSERT INTO planned_workout(id,goal_id,title,planned_date,objective,duration_minutes,intensity,steps_json,rationale,confidence,status,created_at,updated_at,version,previous_workout_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, goal.getId(), result.title(), result.plannedDate(), result.objective(), result.durationMinutes(),
                    result.intensity(), mapper.writeValueAsString(result.steps()), result.rationale(), result.confidence().name(),
                    "PROPOSEE", databaseNow, databaseNow, version, previousId);
        } catch (Exception exception) { throw new IllegalStateException("La séance générée ne peut pas être enregistrée.", exception); }
        return find(id);
    }

    @Transactional
    public NextWorkoutResponse accept(UUID id) {
        Instant now = clock.instant();
        NextWorkoutResponse workout = find(id);
        if (!"PROPOSEE".equals(workout.statut()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une séance proposée peut être acceptée.");
        Timestamp timestamp = Timestamp.from(now);
        jdbc.update("UPDATE planned_workout SET status='REMPLACEE',updated_at=? WHERE status='PLANIFIEE' AND id<>?", timestamp, id);
        jdbc.update("UPDATE planned_workout SET status='PLANIFIEE',decided_at=?,updated_at=? WHERE id=? AND status='PROPOSEE'", timestamp, timestamp, id);
        return find(id);
    }

    @Transactional
    public NextWorkoutResponse refuse(UUID id) {
        NextWorkoutResponse workout = find(id);
        if (!"PROPOSEE".equals(workout.statut()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une séance proposée peut être refusée.");
        Timestamp timestamp = Timestamp.from(clock.instant());
        jdbc.update("UPDATE planned_workout SET status='REFUSEE',decided_at=?,updated_at=? WHERE id=? AND status='PROPOSEE'", timestamp, timestamp, id);
        return find(id);
    }

    @Transactional(readOnly = true)
    public List<NextWorkoutResponse> history() {
        return jdbc.query("SELECT * FROM planned_workout ORDER BY version DESC,created_at DESC", (rs, row) -> map(rs));
    }

    private NextWorkoutResponse find(UUID id) {
        return jdbc.query("SELECT * FROM planned_workout WHERE id=?", (rs, row) -> map(rs), id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable."));
    }

    public boolean automaticEnabled() { return settings.getOrCreate().isAutoGenerateWorkoutAfterImport(); }

    private NextWorkoutResponse map(java.sql.ResultSet rs) throws java.sql.SQLException {
        try {
            return new NextWorkoutResponse(rs.getObject("id", UUID.class), rs.getString("title"),
                    rs.getObject("planned_date", LocalDate.class), "COURSE", rs.getInt("duration_minutes"), null,
                    rs.getString("intensity"), rs.getString("rationale"),
                    mapper.readValue(rs.getString("steps_json"), new TypeReference<>() {}), rs.getString("confidence"), rs.getString("status"),
                    rs.getInt("version"), rs.getObject("previous_workout_id", UUID.class));
        } catch (java.io.IOException exception) { throw new java.sql.SQLException(exception); }
    }
    static List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split("[,;|]"))
                .map(String::trim).filter(v -> !v.isBlank()).toList();
    }

    static LocalDate nextAvailableDate(LocalDate today, List<String> availableDays) {
        if (availableDays.isEmpty()) return today.plusDays(1);
        for (int offset = 1; offset <= 7; offset++) {
            LocalDate candidate = today.plusDays(offset);
            if (isAvailable(candidate, availableDays)) return candidate;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Les jours disponibles du profil ne sont pas reconnus.");
    }

    static void validate(LocalDate date, int duration, LocalDate today, List<String> availableDays, Integer maximumDuration) {
        if (date == null || !date.isAfter(today))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La séance proposée doit être dans le futur.");
        if (maximumDuration != null && duration > maximumDuration)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La séance proposée dépasse la durée maximale du profil.");
        if (!availableDays.isEmpty() && !isAvailable(date, availableDays))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La séance proposée ne respecte pas les jours disponibles.");
    }

    private static boolean isAvailable(LocalDate date, List<String> availableDays) {
        String day = switch (date.getDayOfWeek()) {
            case MONDAY -> "lundi"; case TUESDAY -> "mardi"; case WEDNESDAY -> "mercredi";
            case THURSDAY -> "jeudi"; case FRIDAY -> "vendredi"; case SATURDAY -> "samedi"; case SUNDAY -> "dimanche";
        };
        return availableDays.stream().map(NextWorkoutService::normalize).anyMatch(value -> value.equals(day));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }
}

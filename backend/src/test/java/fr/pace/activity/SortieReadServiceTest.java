package fr.pace.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SortieReadServiceTest {
    @Test
    @DisplayName("Devrait exposer les sorties récentes sans charger les fichiers FIT")
    @SuppressWarnings("unchecked")
    void recent_shouldReturnNewestActivitiesFirst_whenDetailsArePersisted() throws Exception {
        // GIVEN
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(any(String.class), any(RowMapper.class), anyInt())).thenAnswer(invocation -> {
            RowMapper<Object> mapper = invocation.getArgument(1);
            return List.of(
                    map(mapper, "2026-08-19T06:00:00Z", "running", 8_000, 3_000),
                    map(mapper, "2026-08-21T07:00:00Z", "trail_running", 12_500, 4_200)
            );
        });
        SortieReadService service = new SortieReadService(jdbc, new ObjectMapper());

        // WHEN
        List<SortieResponse> result = service.recent(20);

        // THEN
        assertThat(result).extracting(SortieResponse::dateHeure).containsExactly(
                Instant.parse("2026-08-21T07:00:00Z"), Instant.parse("2026-08-19T06:00:00Z"));
        assertThat(result.getFirst()).extracting(SortieResponse::sport, SortieResponse::distanceMetres,
                        SortieResponse::dureeSecondes, SortieResponse::source)
                .containsExactly("trail_running", 12_500L, 4_200L, "GARMIN_PERSONNEL");
    }

    private static Object map(RowMapper<Object> mapper, String date, String sport, long distance, long duration)
            throws Exception {
        ResultSet result = mock(ResultSet.class);
        when(result.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
        when(result.getString("source")).thenReturn("GARMIN_PERSONNEL");
        when(result.getString("details_json")).thenReturn("""
                {"startedAt":"%s","sport":"%s","availableMetrics":{"summaryDTO":{
                "distance":%d,"duration":%d}}}
                """.formatted(date, sport, distance, duration));
        return mapper.mapRow(result, 0);
    }
}

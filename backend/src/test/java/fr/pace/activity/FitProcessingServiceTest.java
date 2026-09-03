package fr.pace.activity;

import com.garmin.fit.DateTime;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FitProcessingServiceTest {
    @TempDir Path temporaryDirectory;
    private JdbcTemplate jdbc;
    private FitProcessingService service;

    @BeforeEach
    void prepareDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:fit-" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE activity(id UUID PRIMARY KEY, original_fit BYTEA, discovered_at TIMESTAMP WITH TIME ZONE)");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V3__decodage_fit.sql"))
                .execute(dataSource);
        service = new FitProcessingService(jdbc, new GarminFitDecoder());
    }

    @Test
    @DisplayName("Devrait décoder puis rejouer une activité sans dupliquer les mesures")
    void processPending_shouldReplaceDecodedData_whenActivityIsReplayed() throws Exception {
        // GIVEN
        UUID id = insert(syntheticFit(), "A_DECODER");

        // WHEN
        service.processPending();
        service.replay(id);
        service.processPending();

        // THEN
        assertThat(text("SELECT fit_decode_status FROM activity WHERE id=?", id)).isEqualTo("DECODEE");
        assertThat(number("SELECT count(*) FROM activity_fit_summary WHERE activity_id=?", id)).isEqualTo(1);
    }

    @Test
    @DisplayName("Devrait conserver une erreur explicite quand le FIT est invalide")
    void processPending_shouldPersistError_whenFitIsInvalid() {
        // GIVEN
        UUID id = insert(new byte[]{1, 2, 3}, "A_DECODER");

        // WHEN
        service.processPending();

        // THEN
        assertThat(text("SELECT fit_decode_status FROM activity WHERE id=?", id)).isEqualTo("ERREUR");
        assertThat(text("SELECT fit_decode_error FROM activity WHERE id=?", id)).contains("invalide");
    }

    private UUID insert(byte[] fit, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO activity(id,original_fit,discovered_at,fit_decode_status) VALUES (?,?,?,?)",
                id, fit, Instant.now(), status);
        return id;
    }

    private byte[] syntheticFit() throws Exception {
        Path path = temporaryDirectory.resolve("traitement.fit");
        FileEncoder encoder = new FileEncoder(path.toFile());
        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(com.garmin.fit.File.ACTIVITY);
        fileId.setManufacturer(1);
        encoder.write(fileId);
        SessionMesg session = new SessionMesg();
        session.setTimestamp(new DateTime(Instant.parse("2026-08-21T08:00:00Z")));
        session.setSport(Sport.RUNNING);
        session.setTotalDistance(10_000F);
        encoder.write(session);
        encoder.close();
        return Files.readAllBytes(path);
    }

    private String text(String sql, UUID id) { return jdbc.queryForObject(sql, String.class, id); }
    private Integer number(String sql, UUID id) { return jdbc.queryForObject(sql, Integer.class, id); }
}

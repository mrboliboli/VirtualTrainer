package fr.pace.activity;

import com.garmin.fit.DateTime;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GarminFitDecoderTest {
    @TempDir Path temporaryDirectory;

    @Test
    @DisplayName("Devrait décoder une session FIT synthétique sans inventer les mesures absentes")
    void decode_shouldExtractAvailableMetrics_whenSyntheticFitIsValid() throws Exception {
        // GIVEN
        Path fixture = temporaryDirectory.resolve("activite-synthetique.fit");
        FileEncoder encoder = new FileEncoder(fixture.toFile());
        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(com.garmin.fit.File.ACTIVITY);
        fileId.setManufacturer(1);
        encoder.write(fileId);
        SessionMesg session = new SessionMesg();
        session.setTimestamp(new DateTime(Instant.parse("2026-08-21T08:00:00Z")));
        session.setStartTime(new DateTime(Instant.parse("2026-08-21T07:00:00Z")));
        session.setSport(Sport.RUNNING);
        session.setTotalDistance(10_000F);
        session.setTotalTimerTime(3_600F);
        session.setAvgHeartRate((short) 150);
        encoder.write(session);
        encoder.close();

        // WHEN
        DecodedFit result = new GarminFitDecoder().decode(Files.readAllBytes(fixture));

        // THEN
        assertThat(result.summary().distance()).isEqualTo(10_000D);
        assertThat(result.summary().startedAt()).isEqualTo(Instant.parse("2026-08-21T07:00:00Z"));
        assertThat(result.summary().active()).isEqualTo(3_600D);
        assertThat(result.summary().avgHr()).isEqualTo((short) 150);
        assertThat(result.summary().avgPower()).isNull();
    }

    @Test
    @DisplayName("Devrait refuser un contenu qui n'est pas un fichier FIT")
    void decode_shouldRejectContent_whenFitIsInvalid() {
        assertThatThrownBy(() -> new GarminFitDecoder().decode(new byte[]{1, 2, 3}))
                .isInstanceOf(FitDecodeException.class)
                .hasMessageContaining("invalide");
    }
}

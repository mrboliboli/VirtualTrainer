package fr.pace.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "pace.fit.enabled", havingValue = "true", matchIfMissing = true)
public class FitProcessingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FitProcessingService.class);
    private static final String DECODER_VERSION = "garmin-fit-21.205.0-v1";
    private final JdbcTemplate jdbc;
    private final GarminFitDecoder decoder;
    private final Clock clock = Clock.systemUTC();

    public FitProcessingService(JdbcTemplate jdbc, GarminFitDecoder decoder) {
        this.jdbc = jdbc;
        this.decoder = decoder;
    }

    @Scheduled(fixedDelayString = "${pace.fit.scan-delay:5000}")
    @Transactional
    public void processPending() {
        List<PendingActivity> pending = jdbc.query(
                "SELECT id, original_fit FROM activity WHERE fit_decode_status='A_DECODER' " +
                        "AND original_fit IS NOT NULL ORDER BY discovered_at LIMIT 5 FOR UPDATE SKIP LOCKED",
                (result, row) -> new PendingActivity(result.getObject("id", UUID.class), result.getBytes("original_fit"))
        );
        pending.forEach(this::process);
    }

    void replay(UUID activityId) {
        jdbc.update("UPDATE activity SET fit_decode_status='A_DECODER', fit_decode_error=NULL WHERE id=?", activityId);
    }

    private void process(PendingActivity pending) {
        jdbc.update("UPDATE activity SET fit_decode_status='EN_COURS', fit_decode_error=NULL WHERE id=?", pending.id());
        try {
            DecodedFit decoded = decoder.decode(pending.fit());
            replaceDecodedData(pending.id(), decoded);
            jdbc.update("UPDATE activity SET fit_decode_status='DECODEE', fit_decoder_version=?, fit_decoded_at=?, " +
                            "fit_decode_error=NULL WHERE id=?", DECODER_VERSION, Timestamp.from(clock.instant()), pending.id());
        } catch (FitDecodeException exception) {
            LOGGER.warn("Décodage FIT impossible pour l'activité {} : {}", pending.id(), exception.getMessage());
            jdbc.update("UPDATE activity SET fit_decode_status='ERREUR', fit_decode_error=? WHERE id=?",
                    exception.getMessage(), pending.id());
        }
    }

    private void replaceDecodedData(UUID activityId, DecodedFit decoded) {
        jdbc.update("DELETE FROM activity_fit_zone WHERE activity_id=?", activityId);
        jdbc.update("DELETE FROM activity_fit_sample WHERE activity_id=?", activityId);
        jdbc.update("DELETE FROM activity_fit_lap WHERE activity_id=?", activityId);
        jdbc.update("DELETE FROM activity_fit_summary WHERE activity_id=?", activityId);
        DecodedFit.Summary s = decoded.summary();
        jdbc.update("INSERT INTO activity_fit_summary(activity_id,started_at,sport,sub_sport,distance_meters," +
                        "elapsed_seconds,active_seconds,average_speed_mps,maximum_speed_mps,average_heart_rate," +
                        "maximum_heart_rate,average_cadence,maximum_cadence,average_power,maximum_power,normalized_power," +
                        "calories,ascent_meters,descent_meters,aerobic_training_effect,anaerobic_training_effect,training_load) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                activityId, timestamp(s.startedAt()), s.sport(), s.subSport(), s.distance(), s.elapsed(), s.active(),
                s.avgSpeed(), s.maxSpeed(), s.avgHr(), s.maxHr(), s.avgCadence(), s.maxCadence(), s.avgPower(),
                s.maxPower(), s.normalizedPower(), s.calories(), s.ascent(), s.descent(), s.aerobicEffect(),
                s.anaerobicEffect(), s.trainingLoad());
        decoded.laps().forEach(l -> jdbc.update("INSERT INTO activity_fit_lap(id,activity_id,lap_index,started_at," +
                        "distance_meters,elapsed_seconds,active_seconds,average_heart_rate,maximum_heart_rate,average_power) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), activityId, l.index(), timestamp(l.startedAt()),
                l.distance(), l.elapsed(), l.active(), l.avgHr(), l.maxHr(), l.avgPower()));
        decoded.samples().forEach(sam -> jdbc.update("INSERT INTO activity_fit_sample(id,activity_id,sample_index," +
                        "recorded_at,heart_rate,power,cadence,altitude_meters,speed_mps,temperature_celsius,respiration_rate) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), activityId, sam.index(),
                timestamp(sam.recordedAt()), sam.heartRate(), sam.power(), sam.cadence(), sam.altitude(), sam.speed(),
                sam.temperature(), sam.respiration()));
        decoded.zones().forEach(zone -> jdbc.update("INSERT INTO activity_fit_zone(id,activity_id,zone_type,zone_index," +
                        "lower_bound,upper_bound,duration_seconds) VALUES (?,?,?,?,?,?,?)", UUID.randomUUID(), activityId,
                zone.type(), zone.index(), zone.lower(), zone.upper(), zone.duration()));
    }

    private static Timestamp timestamp(java.time.Instant value) { return value == null ? null : Timestamp.from(value); }
    private record PendingActivity(UUID id, byte[] fit) { }
}

package fr.pace.activity;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class SortieDetailService {
    private static final int MAX_SAMPLES_EXPOSED = 10_000;
    private final JdbcTemplate jdbc;

    public SortieDetailService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public SortieDetailResponse get(UUID id) {
        try {
            SortieDetailResponse base = jdbc.queryForObject("SELECT a.id,a.source,a.fit_decode_status,a.fit_decode_error," +
                    "a.perceived_exertion_rpe,a.garmin_feeling_score,a.subjective_feedback_source," +
                    "s.* FROM activity a LEFT JOIN activity_fit_summary s ON s.activity_id=a.id WHERE a.id=?",
                    (result, row) -> summary(result), id);
            List<SortieDetailResponse.TourResponse> laps = jdbc.query("SELECT * FROM activity_fit_lap WHERE activity_id=? " +
                            "ORDER BY lap_index LIMIT 2000", (r, row) -> new SortieDetailResponse.TourResponse(
                    r.getInt("lap_index"), instant(r, "started_at"), number(r, "distance_meters"),
                    number(r, "elapsed_seconds"), number(r, "active_seconds"), shortNumber(r, "average_heart_rate"),
                    shortNumber(r, "maximum_heart_rate"), integer(r, "average_power")), id);
            List<SortieDetailResponse.ZoneResponse> zones = jdbc.query("SELECT * FROM activity_fit_zone WHERE activity_id=? " +
                            "ORDER BY zone_type,zone_index", (r, row) -> new SortieDetailResponse.ZoneResponse(
                    r.getString("zone_type"), r.getInt("zone_index"), number(r, "lower_bound"),
                    number(r, "upper_bound"), number(r, "duration_seconds")), id);
            List<SortieDetailResponse.EchantillonResponse> samples = jdbc.query("SELECT * FROM activity_fit_sample " +
                            "WHERE activity_id=? ORDER BY sample_index LIMIT " + MAX_SAMPLES_EXPOSED,
                    (r, row) -> new SortieDetailResponse.EchantillonResponse(instant(r, "recorded_at"),
                            shortNumber(r, "heart_rate"), integer(r, "power"), shortNumber(r, "cadence"),
                            number(r, "altitude_meters")), id);
            Long totalSamples = jdbc.queryForObject(
                    "SELECT count(*) FROM activity_fit_sample WHERE activity_id=?",
                    Long.class,
                    id
            );
            long sampleCount = totalSamples == null ? 0 : totalSamples;
            CompteRenduFactuelResponse report = CompteRenduFactuelCalculator.calculate(
                    base.distanceMetres(), base.dureeActiveSecondes(), zones,
                    samples.stream().map(sample -> new CompteRenduFactuelCalculator.Sample(
                            sample.frequenceCardiaque(), sample.puissanceWatts(), sample.cadence()
                    )).toList()
            );
            return new SortieDetailResponse(base.id(), base.dateHeure(), base.sport(), base.sousSport(), base.source(),
                    base.etatDecodage(), base.erreurDecodage(), base.distanceMetres(), base.dureeEcouleeSecondes(),
                    base.dureeActiveSecondes(), base.vitesseMoyenneMetresParSeconde(), base.vitesseMaximaleMetresParSeconde(),
                    base.frequenceCardiaqueMoyenne(), base.frequenceCardiaqueMaximale(), base.cadenceMoyenne(),
                    base.cadenceMaximale(), base.puissanceMoyenneWatts(), base.puissanceMaximaleWatts(),
                    base.puissanceNormaliseeWatts(), base.calories(), base.denivelePositifMetres(),
                    base.deniveleNegatifMetres(), base.effetEntrainementAerobie(), base.effetEntrainementAnaerobie(),
                    base.chargeEntrainement(), base.ressenti(), report,
                    laps, zones, samples, sampleCount, sampleCount > samples.size());
        } catch (EmptyResultDataAccessException exception) {
            throw new SortieNotFoundException(id);
        }
    }

    private static SortieDetailResponse summary(ResultSet r) throws SQLException {
        return new SortieDetailResponse(r.getObject("id", UUID.class), instant(r, "started_at"), r.getString("sport"),
                r.getString("sub_sport"), r.getString("source"), r.getString("fit_decode_status"),
                r.getString("fit_decode_error"), number(r, "distance_meters"), number(r, "elapsed_seconds"),
                number(r, "active_seconds"), number(r, "average_speed_mps"), number(r, "maximum_speed_mps"),
                shortNumber(r, "average_heart_rate"), shortNumber(r, "maximum_heart_rate"),
                shortNumber(r, "average_cadence"), shortNumber(r, "maximum_cadence"), integer(r, "average_power"),
                integer(r, "maximum_power"), integer(r, "normalized_power"), integer(r, "calories"),
                integer(r, "ascent_meters"), integer(r, "descent_meters"), number(r, "aerobic_training_effect"),
                number(r, "anaerobic_training_effect"), number(r, "training_load"),
                new SortieDetailResponse.RessentiResponse(number(r, "perceived_exertion_rpe"),
                        number(r, "garmin_feeling_score"), r.getString("subjective_feedback_source")),
                null, List.of(), List.of(), List.of(), 0, false);
    }
    private static java.time.Instant instant(ResultSet r, String field) throws SQLException {
        java.sql.Timestamp value = r.getTimestamp(field); return value == null ? null : value.toInstant();
    }
    private static Double number(ResultSet r, String field) throws SQLException { double v=r.getDouble(field); return r.wasNull()?null:v; }
    private static Integer integer(ResultSet r, String field) throws SQLException { int v=r.getInt(field); return r.wasNull()?null:v; }
    private static Short shortNumber(ResultSet r, String field) throws SQLException { short v=r.getShort(field); return r.wasNull()?null:v; }
}

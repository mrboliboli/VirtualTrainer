package fr.pace.activity;

import com.garmin.fit.Decode;
import com.garmin.fit.LapMesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class GarminFitDecoder {
    static final int MAX_FIT_BYTES = 10 * 1024 * 1024;
    static final int MAX_LAPS = 2_000;
    static final int MAX_SAMPLES = 200_000;
    static final int MAX_SESSIONS = 10;
    static final int MAX_ZONES = 100;

    DecodedFit decode(byte[] content) {
        if (content == null || content.length == 0) throw new FitDecodeException("Le fichier FIT est vide.");
        if (content.length > MAX_FIT_BYTES) throw new FitDecodeException("Le fichier FIT dépasse la taille autorisée.");
        try {
            Decode integrityDecoder = new Decode();
            if (!integrityDecoder.checkFileIntegrity(new ByteArrayInputStream(content))) {
                throw new FitDecodeException("Le fichier FIT est incomplet ou invalide.");
            }
        } catch (FitDecodeException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new FitDecodeException("Le fichier FIT est incomplet ou invalide.");
        }
        List<SessionMesg> sessions = new ArrayList<>();
        List<LapMesg> laps = new ArrayList<>();
        List<RecordMesg> records = new ArrayList<>();
        List<DecodedFit.Zone> zones = new ArrayList<>();
        try {
            MesgBroadcaster broadcaster = new MesgBroadcaster(new Decode());
            broadcaster.addListener((com.garmin.fit.SessionMesgListener) message -> addBounded(sessions, message, MAX_SESSIONS));
            broadcaster.addListener((com.garmin.fit.LapMesgListener) message -> addBounded(laps, message, MAX_LAPS));
            broadcaster.addListener((com.garmin.fit.RecordMesgListener) message -> addBounded(records, message, MAX_SAMPLES));
            broadcaster.addListener((com.garmin.fit.TimeInZoneMesgListener) message -> {
                addZones(zones, "CARDIAQUE", message.getTimeInHrZone());
                addZones(zones, "PUISSANCE", message.getTimeInPowerZone());
            });
            broadcaster.run(new ByteArrayInputStream(content));
        } catch (RuntimeException exception) {
            throw new FitDecodeException("Le fichier FIT est incomplet ou invalide.", exception);
        }
        if (sessions.isEmpty()) throw new FitDecodeException("Le fichier FIT ne contient aucune session.");
        SessionMesg session = sessions.getFirst();
        DecodedFit.Summary summary = new DecodedFit.Summary(
                instant(session.getStartTime(), session.getTimestamp()), text(session.getSport()), text(session.getSubSport()),
                decimal(session.getTotalDistance()), decimal(session.getTotalElapsedTime()), decimal(session.getTotalTimerTime()),
                decimal(session.getEnhancedAvgSpeed(), session.getAvgSpeed()), decimal(session.getEnhancedMaxSpeed(), session.getMaxSpeed()),
                session.getAvgHeartRate(), session.getMaxHeartRate(), session.getAvgCadence(), session.getMaxCadence(),
                session.getAvgPower(), session.getMaxPower(), session.getNormalizedPower(), session.getTotalCalories(),
                session.getTotalAscent(), session.getTotalDescent(), decimal(session.getTotalTrainingEffect()),
                decimal(session.getTotalAnaerobicTrainingEffect()), decimal(session.getTrainingLoadPeak())
        );
        List<DecodedFit.Lap> decodedLaps = java.util.stream.IntStream.range(0, laps.size())
                .mapToObj(index -> lap(index, laps.get(index))).toList();
        List<DecodedFit.Sample> samples = java.util.stream.IntStream.range(0, records.size())
                .mapToObj(index -> sample(index, records.get(index))).toList();
        return new DecodedFit(summary, decodedLaps, samples, List.copyOf(zones));
    }

    private static DecodedFit.Lap lap(int index, LapMesg value) {
        return new DecodedFit.Lap(index, instant(value.getStartTime(), value.getTimestamp()), decimal(value.getTotalDistance()),
                decimal(value.getTotalElapsedTime()), decimal(value.getTotalTimerTime()), value.getAvgHeartRate(),
                value.getMaxHeartRate(), value.getAvgPower());
    }

    private static DecodedFit.Sample sample(int index, RecordMesg value) {
        return new DecodedFit.Sample(index, instant(value.getTimestamp()), value.getHeartRate(), value.getPower(),
                value.getCadence(), decimal(value.getEnhancedAltitude(), value.getAltitude()),
                decimal(value.getEnhancedSpeed(), value.getSpeed()), value.getTemperature(),
                decimal(value.getEnhancedRespirationRate(), value.getRespirationRate()));
    }

    private static <T> void addBounded(List<T> values, T value, int maximum) {
        if (values.size() >= maximum) throw new FitDecodeException("Le fichier FIT contient trop de mesures.");
        values.add(value);
    }
    private static void addZones(List<DecodedFit.Zone> zones, String type, Float[] durations) {
        if (durations == null) return;
        for (int index = 0; index < durations.length; index++) {
            if (durations[index] == null) continue;
            int zoneIndex = index;
            int existingIndex = java.util.stream.IntStream.range(0, zones.size())
                    .filter(position -> zones.get(position).type().equals(type)
                            && zones.get(position).index() == zoneIndex)
                    .findFirst()
                    .orElse(-1);
            DecodedFit.Zone candidate = new DecodedFit.Zone(
                    type,
                    index,
                    null,
                    null,
                    durations[index].doubleValue()
            );
            if (existingIndex < 0) {
                addBounded(zones, candidate, MAX_ZONES);
            } else if (candidate.duration() > zones.get(existingIndex).duration()) {
                zones.set(existingIndex, candidate); // le total de session prime sur les sous-totaux de tours
            }
        }
    }
    private static java.time.Instant instant(com.garmin.fit.DateTime value) { return value == null ? null : value.getInstant(); }
    private static java.time.Instant instant(com.garmin.fit.DateTime preferred, com.garmin.fit.DateTime fallback) {
        return instant(preferred != null ? preferred : fallback);
    }
    private static String text(Object value) { return value == null ? null : value.toString(); }
    private static Double decimal(Number value) { return value == null ? null : value.doubleValue(); }
    private static Double decimal(Number preferred, Number fallback) { return decimal(preferred != null ? preferred : fallback); }
}

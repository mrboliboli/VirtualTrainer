package fr.pace.activity;

import java.time.Instant;
import java.util.List;

record DecodedFit(Summary summary, List<Lap> laps, List<Sample> samples, List<Zone> zones) {
    record Summary(Instant startedAt, String sport, String subSport, Double distance,
                   Double elapsed, Double active, Double avgSpeed, Double maxSpeed,
                   Short avgHr, Short maxHr, Short avgCadence, Short maxCadence,
                   Integer avgPower, Integer maxPower, Integer normalizedPower,
                   Integer calories, Integer ascent, Integer descent,
                   Double aerobicEffect, Double anaerobicEffect, Double trainingLoad) { }
    record Lap(int index, Instant startedAt, Double distance, Double elapsed, Double active,
               Short avgHr, Short maxHr, Integer avgPower) { }
    record Sample(int index, Instant recordedAt, Short heartRate, Integer power, Short cadence,
                  Double altitude, Double speed, Byte temperature, Double respiration) { }
    record Zone(String type, int index, Double lower, Double upper, Double duration) { }
}

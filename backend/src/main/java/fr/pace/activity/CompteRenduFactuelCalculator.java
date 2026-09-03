package fr.pace.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class CompteRenduFactuelCalculator {
    private CompteRenduFactuelCalculator() { }

    static CompteRenduFactuelResponse calculate(
            Double distanceMeters,
            Double activeSeconds,
            List<SortieDetailResponse.ZoneResponse> zones,
            List<Sample> samples
    ) {
        Double pace = positive(distanceMeters) && positive(activeSeconds)
                ? activeSeconds / (distanceMeters / 1_000.0) : null;
        var heartRate = evolution(samples, Sample::heartRate);
        var power = evolution(samples, Sample::power);
        var cadence = evolution(samples, Sample::cadence);
        List<Double> powerValues = values(samples, Sample::power);
        Double powerVariation = coefficientOfVariation(powerValues);
        List<String> missing = new ArrayList<>();
        if (pace == null) missing.add("Allure moyenne : distance ou durée active absente.");
        if (heartRate == null) missing.add("Évolution cardiaque : pas assez de relevés dans chaque moitié.");
        if (power == null) missing.add("Évolution de puissance : pas assez de relevés dans chaque moitié.");
        if (cadence == null) missing.add("Évolution de cadence : pas assez de relevés dans chaque moitié.");
        if (powerVariation == null) missing.add("Régularité de puissance : pas assez de relevés de puissance.");
        if (zones.isEmpty()) missing.add("Répartition par zones : aucune durée de zone enregistrée.");
        return new CompteRenduFactuelResponse(
                pace, heartRate, power, cadence, powerVariation, confidence(powerValues.size()),
                zoneDistribution(zones), List.copyOf(missing)
        );
    }

    private static CompteRenduFactuelResponse.EvolutionResponse evolution(
            List<Sample> samples,
            Function<Sample, Number> extractor
    ) {
        int middle = samples.size() / 2;
        if (middle == 0) return null;
        List<Double> first = values(samples.subList(0, middle), extractor);
        List<Double> second = values(samples.subList(middle, samples.size()), extractor);
        if (first.size() < 20 || second.size() < 20) return null;
        double firstAverage = average(first);
        double secondAverage = average(second);
        Double change = firstAverage == 0 ? null : (secondAverage - firstAverage) / firstAverage * 100;
        return new CompteRenduFactuelResponse.EvolutionResponse(
                firstAverage, secondAverage, change, confidence(Math.min(first.size(), second.size()) * 2)
        );
    }

    private static List<CompteRenduFactuelResponse.RepartitionZoneResponse> zoneDistribution(
            List<SortieDetailResponse.ZoneResponse> zones
    ) {
        double total = zones.stream().map(SortieDetailResponse.ZoneResponse::dureeSecondes)
                .filter(Objects::nonNull).filter(value -> value > 0).mapToDouble(Double::doubleValue).sum();
        return zones.stream().map(zone -> new CompteRenduFactuelResponse.RepartitionZoneResponse(
                zone.type(), zone.index(), zone.dureeSecondes(),
                total > 0 && zone.dureeSecondes() != null ? zone.dureeSecondes() / total * 100 : null
        )).toList();
    }

    private static <T extends Number> List<Double> values(
            List<Sample> samples,
            Function<Sample, T> extractor
    ) {
        return samples.stream().map(extractor).filter(Objects::nonNull)
                .map(Number::doubleValue).filter(Double::isFinite).toList();
    }

    private static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private static Double coefficientOfVariation(List<Double> values) {
        if (values.size() < 20) return null;
        double average = average(values);
        if (average == 0) return null;
        double variance = values.stream().mapToDouble(value -> Math.pow(value - average, 2)).average().orElse(0);
        return Math.sqrt(variance) / average * 100;
    }

    private static String confidence(int samples) {
        if (samples >= 300) return "ELEVEE";
        if (samples >= 60) return "MOYENNE";
        return samples >= 20 ? "FAIBLE" : null;
    }

    private static boolean positive(Double value) {
        return value != null && Double.isFinite(value) && value > 0;
    }

    record Sample(Short heartRate, Integer power, Short cadence) { }
}

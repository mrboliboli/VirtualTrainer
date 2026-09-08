package fr.pace.ai.application;

import fr.pace.activity.SortieDetailResponse;
import fr.pace.ai.domain.ActivityAnalysisRequest;
import fr.pace.ai.domain.ConfidenceLevel;
import fr.pace.goal.DistanceUnit;
import fr.pace.goal.GoalRepository;
import fr.pace.profile.AthleteProfileService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ActivityAnalysisRequestFactory {
    private final AthleteProfileService profiles;
    private final GoalRepository goals;
    private final Clock clock = Clock.systemUTC();

    public ActivityAnalysisRequestFactory(AthleteProfileService profiles, GoalRepository goals) {
        this.profiles = profiles; this.goals = goals;
    }

    public ActivityAnalysisRequest create(SortieDetailResponse sortie) {
        var profile = profiles.get();
        Integer age = profile.getBirthYear() == null ? null : LocalDate.now(clock).getYear() - profile.getBirthYear();
        var athlete = new ActivityAnalysisRequest.AthleteContext(age, null,
                split(profile.getAvailableDays()).size(), split(profile.getAvailableTerrains()));
        var goal = goals.findAllByPrimaryTrueAndArchivedFalse().stream().findFirst().map(value ->
                new ActivityAnalysisRequest.GoalContext(value.getType().jsonValue(),
                        value.getEventDate().atStartOfDay().toInstant(ZoneOffset.UTC),
                        value.getDistance() == null ? null : value.getDistance().doubleValue()
                                * (value.getDistanceUnit() == DistanceUnit.MILE ? 1609.344 : 1000),
                        value.getTargetTimeSeconds())).orElse(null);
        Map<String, Double> metrics = new LinkedHashMap<>();
        put(metrics, "frequenceCardiaqueMoyenne", sortie.frequenceCardiaqueMoyenne());
        put(metrics, "frequenceCardiaqueMaximale", sortie.frequenceCardiaqueMaximale());
        put(metrics, "puissanceMoyenneWatts", sortie.puissanceMoyenneWatts());
        put(metrics, "puissanceNormaliseeWatts", sortie.puissanceNormaliseeWatts());
        put(metrics, "cadenceMoyenne", sortie.cadenceMoyenne());
        put(metrics, "effetEntrainementAerobie", sortie.effetEntrainementAerobie());
        put(metrics, "effetEntrainementAnaerobie", sortie.effetEntrainementAnaerobie());
        put(metrics, "chargeEntrainement", sortie.chargeEntrainement());
        var activity = new ActivityAnalysisRequest.ActivityContext(sortie.dateHeure(), sortie.sport(),
                sortie.distanceMetres(), sortie.dureeEcouleeSecondes(), sortie.dureeActiveSecondes(),
                sortie.denivelePositifMetres(), metrics);
        var feedback = sortie.ressenti() == null ? null : new ActivityAnalysisRequest.SubjectiveFeedback(
                sortie.ressenti().rpeSurDix(), sortie.ressenti().scoreGarminSurCent(), sortie.ressenti().source());
        var laps = sortie.tours().stream().map(value -> new ActivityAnalysisRequest.LapAggregate(value.index(),
                value.distanceMetres(), value.dureeActiveSecondes(), number(value.frequenceCardiaqueMoyenne()),
                number(value.puissanceMoyenneWatts()), null)).toList();
        var report = sortie.compteRenduFactuel();
        var zones = report == null ? List.<ActivityAnalysisRequest.ZoneAggregate>of() : report.repartitionZones().stream()
                .map(value -> new ActivityAnalysisRequest.ZoneAggregate(value.type(), value.index(), value.pourcentage())).toList();
        List<ActivityAnalysisRequest.AnalysisFact> facts = facts(sortie);
        List<String> missing = report == null ? List.of("Compte rendu factuel indisponible") : report.donneesAbsentes();
        return new ActivityAnalysisRequest(athlete, goal, activity, feedback, laps, zones, facts, missing, List.of());
    }

    private static List<ActivityAnalysisRequest.AnalysisFact> facts(SortieDetailResponse s) {
        List<ActivityAnalysisRequest.AnalysisFact> result = new ArrayList<>();
        fact(result, "distance", "Distance", s.distanceMetres(), "m");
        fact(result, "duree_active", "Durée active", s.dureeActiveSecondes(), "s");
        fact(result, "fc_moyenne", "Fréquence cardiaque moyenne", s.frequenceCardiaqueMoyenne(), "bpm");
        fact(result, "puissance_moyenne", "Puissance moyenne", s.puissanceMoyenneWatts(), "W");
        if (s.compteRenduFactuel() != null) {
            fact(result, "allure", "Allure calculée", s.compteRenduFactuel().allureMoyenneSecondesParKilometre(), "s/km");
            fact(result, "regularite_puissance", "Dispersion de puissance", s.compteRenduFactuel().regularitePuissanceCoefficientVariationPourcent(), "%");
        }
        if (result.isEmpty()) result.add(new ActivityAnalysisRequest.AnalysisFact("activite", "Activité", "mesures limitées", ConfidenceLevel.FAIBLE));
        return result;
    }
    private static void fact(List<ActivityAnalysisRequest.AnalysisFact> result, String id, String label, Number value, String unit) {
        if (value != null) result.add(new ActivityAnalysisRequest.AnalysisFact(id, label, value + " " + unit, ConfidenceLevel.ELEVE));
    }
    private static void put(Map<String, Double> map, String key, Number value) { if (value != null) map.put(key, value.doubleValue()); }
    private static Double number(Number value) { return value == null ? null : value.doubleValue(); }
    private static List<String> split(String value) { return value == null || value.isBlank() ? List.of() : List.of(value.split("\\|")); }
}

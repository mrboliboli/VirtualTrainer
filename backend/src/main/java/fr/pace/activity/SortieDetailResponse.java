package fr.pace.activity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SortieDetailResponse(
        UUID id, Instant dateHeure, String sport, String sousSport, String source,
        String etatDecodage, String erreurDecodage,
        Double distanceMetres, Double dureeEcouleeSecondes, Double dureeActiveSecondes,
        Double vitesseMoyenneMetresParSeconde, Double vitesseMaximaleMetresParSeconde,
        Short frequenceCardiaqueMoyenne, Short frequenceCardiaqueMaximale,
        Short cadenceMoyenne, Short cadenceMaximale,
        Integer puissanceMoyenneWatts, Integer puissanceMaximaleWatts, Integer puissanceNormaliseeWatts,
        Integer calories, Integer denivelePositifMetres, Integer deniveleNegatifMetres,
        Double effetEntrainementAerobie, Double effetEntrainementAnaerobie, Double chargeEntrainement,
        RessentiResponse ressenti,
        CompteRenduFactuelResponse compteRenduFactuel,
        List<TourResponse> tours, List<ZoneResponse> zones, List<EchantillonResponse> serie,
        long totalEchantillons, boolean serieTronquee
) {
    public record RessentiResponse(Double rpeSurDix, Double scoreGarminSurCent, String source) { }
    public record TourResponse(int index, Instant dateHeure, Double distanceMetres,
                               Double dureeEcouleeSecondes, Double dureeActiveSecondes,
                               Short frequenceCardiaqueMoyenne, Short frequenceCardiaqueMaximale,
                               Integer puissanceMoyenneWatts) { }
    public record ZoneResponse(String type, int index, Double borneBasse, Double borneHaute,
                               Double dureeSecondes) { }
    public record EchantillonResponse(Instant dateHeure, Short frequenceCardiaque,
                                      Integer puissanceWatts, Short cadence,
                                      Double altitudeMetres) { }
}

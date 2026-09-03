package fr.pace.activity;

import java.util.List;

public record CompteRenduFactuelResponse(
        Double allureMoyenneSecondesParKilometre,
        EvolutionResponse frequenceCardiaque,
        EvolutionResponse puissance,
        EvolutionResponse cadence,
        Double regularitePuissanceCoefficientVariationPourcent,
        String confianceRegularitePuissance,
        List<RepartitionZoneResponse> repartitionZones,
        List<String> donneesAbsentes
) {
    public record EvolutionResponse(
            Double premiereMoitie,
            Double secondeMoitie,
            Double evolutionPourcent,
            String confiance
    ) { }

    public record RepartitionZoneResponse(
            String type,
            int index,
            Double dureeSecondes,
            Double pourcentage
    ) { }
}

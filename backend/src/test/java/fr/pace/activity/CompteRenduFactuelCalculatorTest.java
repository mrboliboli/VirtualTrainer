package fr.pace.activity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CompteRenduFactuelCalculatorTest {
    @Test
    @DisplayName("Devrait calculer les évolutions par moitié et la répartition des zones")
    void calculate_shouldReturnObjectiveMetrics_whenSamplesAreSufficient() {
        var samples = IntStream.range(0, 100)
                .mapToObj(index -> new CompteRenduFactuelCalculator.Sample(
                        (short) (index < 50 ? 140 : 147),
                        index % 2 == 0 ? 200 : 220,
                        (short) (index < 50 ? 170 : 172)
                )).toList();
        var zones = List.of(
                new SortieDetailResponse.ZoneResponse("FREQUENCE_CARDIAQUE", 1, null, null, 600.0),
                new SortieDetailResponse.ZoneResponse("FREQUENCE_CARDIAQUE", 2, null, null, 1_800.0)
        );

        CompteRenduFactuelResponse result = CompteRenduFactuelCalculator.calculate(
                10_000.0, 3_000.0, zones, samples
        );

        assertThat(result.allureMoyenneSecondesParKilometre()).isEqualTo(300);
        assertThat(result.frequenceCardiaque().evolutionPourcent()).isEqualTo(5);
        assertThat(result.regularitePuissanceCoefficientVariationPourcent()).isPositive();
        assertThat(result.repartitionZones()).extracting(
                CompteRenduFactuelResponse.RepartitionZoneResponse::pourcentage
        ).containsExactly(25.0, 75.0);
        assertThat(result.confianceRegularitePuissance()).isEqualTo("MOYENNE");
    }

    @Test
    @DisplayName("Devrait expliquer les calculs impossibles plutôt que compléter les données")
    void calculate_shouldDeclareMissingData_whenSamplesAreAbsent() {
        CompteRenduFactuelResponse result = CompteRenduFactuelCalculator.calculate(
                null, null, List.of(), List.of()
        );

        assertThat(result.allureMoyenneSecondesParKilometre()).isNull();
        assertThat(result.frequenceCardiaque()).isNull();
        assertThat(result.donneesAbsentes()).hasSize(6);
    }
}

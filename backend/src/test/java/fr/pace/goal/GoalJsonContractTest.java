package fr.pace.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GoalJsonContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest(name = "{1}")
    @MethodSource("provideJsonValues")
    @DisplayName("Devrait convertir chaque valeur du contrat JSON public")
    void enums_shouldMapBothWays_whenUsingPublicJsonValue(Enum<?> expected, String jsonValue) throws Exception {
        // WHEN
        Enum<?> deserialized = objectMapper.readValue(
                "\"%s\"".formatted(jsonValue),
                expected.getDeclaringClass()
        );
        String serialized = objectMapper.writeValueAsString(expected);

        // THEN
        assertThat(deserialized).isEqualTo(expected);
        assertThat(serialized).isEqualTo("\"%s\"".formatted(jsonValue));
    }

    private static Stream<Arguments> provideJsonValues() {
        return Stream.of(
                Arguments.of(DistanceUnit.KILOMETER, "KM"),
                Arguments.of(DistanceUnit.MILE, "MILES"),
                Arguments.of(GoalType.RUNNING, "RUNNING"),
                Arguments.of(GoalType.TRAIL, "TRAIL"),
                Arguments.of(GoalType.ROUTE, "ROUTE"),
                Arguments.of(GoalType.FIVE_KILOMETERS, "5_KM"),
                Arguments.of(GoalType.TEN_KILOMETERS, "10_KM"),
                Arguments.of(GoalType.HALF_MARATHON, "SEMI_MARATHON"),
                Arguments.of(GoalType.MARATHON, "MARATHON"),
                Arguments.of(GoalType.OTHER, "AUTRE"),
                Arguments.of(GoalStatus.PLANNED, "PREVU"),
                Arguments.of(GoalStatus.ACTIVE, "ACTIF"),
                Arguments.of(GoalStatus.ACHIEVED, "ATTEINT"),
                Arguments.of(GoalStatus.ABANDONED, "ABANDONNE")
        );
    }
}

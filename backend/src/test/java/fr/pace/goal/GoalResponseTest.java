package fr.pace.goal;

import fr.pace.profile.AthleteProfile;
import fr.pace.profile.AthleteProfileData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GoalResponseTest {
    @Test
    @DisplayName("Devrait calculer les jours restants depuis la date courante")
    void from_shouldComputeRemainingDays_whenGoalIsInFuture() {
        // GIVEN
        Instant now = Instant.parse("2026-08-21T10:00:00Z");
        AthleteProfile profile = AthleteProfile.create(UUID.randomUUID(), profileData(), now);
        Goal goal = Goal.create(UUID.randomUUID(), profile, goalData(LocalDate.of(2026, 8, 31)), now);

        // WHEN
        GoalResponse response = GoalResponse.from(goal, Clock.fixed(now, ZoneOffset.UTC));

        // THEN
        assertThat(response.joursRestants()).isEqualTo(10);
    }

    private static AthleteProfileData profileData() {
        return new AthleteProfileData("Fabien", null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static GoalData goalData(LocalDate date) {
        return new GoalData("Course test", date, new BigDecimal("10"), DistanceUnit.KILOMETER,
                GoalType.TEN_KILOMETERS, "https://example.org/course", 10, null, null, null, GoalStatus.ACTIVE, true);
    }
}

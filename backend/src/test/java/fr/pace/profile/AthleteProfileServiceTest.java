package fr.pace.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AthleteProfileServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");
    @Mock private AthleteProfileRepository repository;

    @Test
    @DisplayName("Devrait créer le profil quand aucun profil n'existe")
    void save_shouldCreateProfile_whenProfileDoesNotExist() {
        // GIVEN
        AthleteProfileData data = profileData("Fabien");
        when(repository.count()).thenReturn(0L);
        when(repository.save(any(AthleteProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AthleteProfileService service = service();

        // WHEN
        AthleteProfile result = service.save(data);

        // THEN
        assertThat(result.getFirstName()).isEqualTo("Fabien");
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Devrait signaler un profil absent quand aucun profil n'est configuré")
    void get_shouldThrowDedicatedException_whenProfileDoesNotExist() {
        // GIVEN
        when(repository.findAll()).thenReturn(List.of());

        // WHEN / THEN
        assertThatThrownBy(service()::get).isInstanceOf(ProfileNotConfiguredException.class);
    }

    private AthleteProfileService service() {
        return new AthleteProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static AthleteProfileData profileData(String firstName) {
        return new AthleteProfileData(
                firstName, 1980, 180, new BigDecimal("75.5"), 190, 50, 170, 300,
                new BigDecimal("40"), "Mardi, jeudi, dimanche", 120,
                "Route et sentier", null, "Trois sorties", null
        );
    }
}

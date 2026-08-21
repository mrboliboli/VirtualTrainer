package fr.pace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaceApplicationTest {

    @Test
    @DisplayName("Devrait charger le contexte complet de l'application")
    void contextLoads_shouldStartApplication_whenConfigurationIsValid() {
        // Le chargement du contexte constitue l'assertion.
    }
}

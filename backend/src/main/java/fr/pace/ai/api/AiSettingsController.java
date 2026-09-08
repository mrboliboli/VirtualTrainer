package fr.pace.ai.api;

import fr.pace.ai.application.AiSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reglages/ia")
public class AiSettingsController {
    private final AiSettingsService service;
    public AiSettingsController(AiSettingsService service) { this.service = service; }

    @GetMapping public AiSettingsResponse get() {
        return AiSettingsResponse.from(service.getOrCreate(), service.keyConfigured());
    }
    @PutMapping public AiSettingsResponse update(@Valid @RequestBody AiSettingsRequest request) {
        return AiSettingsResponse.from(service.update(request), service.keyConfigured());
    }
    @PostMapping("/test") public AiSettingsResponse test() {
        var configuration = service.test();
        if ("ECHEC".equals(configuration.getLastTestStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "La connexion au fournisseur IA a échoué.");
        }
        return AiSettingsResponse.from(configuration, service.keyConfigured());
    }
}

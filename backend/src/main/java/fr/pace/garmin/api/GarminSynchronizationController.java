package fr.pace.garmin.api;

import fr.pace.garmin.synchronization.GarminSynchronizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/garmin/synchronisations")
public class GarminSynchronizationController {
    private final GarminSynchronizationService service;

    public GarminSynchronizationController(GarminSynchronizationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SynchronizationResponse start(
            @RequestHeader(name = "X-Cle-Idempotence", required = false) String idempotencyKey
    ) {
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString()
                : idempotencyKey;
        return SynchronizationResponse.from(service.start(key));
    }

    @GetMapping("/{id}")
    public SynchronizationResponse get(@PathVariable UUID id) {
        return SynchronizationResponse.from(service.get(id));
    }

    @PostMapping("/{id}/confirmation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable UUID id, @Valid @RequestBody ConfirmationRequest request) {
        service.confirm(id, request.idExterne());
    }
}

package fr.pace.garmin.api;

import fr.pace.garmin.GarminActivityClient;
import fr.pace.garmin.GarminSession;
import fr.pace.garmin.GarminSessionState;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/garmin/connexion")
public class GarminConnectionController {
    private final GarminActivityClient client;

    public GarminConnectionController(GarminActivityClient client) {
        this.client = client;
    }

    @GetMapping
    public GarminConnectionResponse status() {
        return response(client.sessionStatus(UUID.randomUUID().toString()), null);
    }

    @PostMapping
    public GarminConnectionResponse connect(@Valid @RequestBody GarminConnectionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        GarminSession session = client.connect(request.identifiant(), request.motDePasse(), correlationId);
        return response(session, session.state() == GarminSessionState.MFA_REQUIRED ? correlationId : null);
    }

    @PostMapping("/mfa")
    public GarminConnectionResponse completeMfa(@Valid @RequestBody GarminMfaRequest request) {
        return response(client.completeMfa(request.code(), request.defiMfaId()), null);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect() {
        client.disconnect();
    }

    private static GarminConnectionResponse response(GarminSession session, String challengeId) {
        String status = switch (session.state()) {
            case ABSENT -> "DECONNECTE";
            case MFA_REQUIRED -> "MFA_REQUIS";
            case CONNECTED -> "CONNECTE";
        };
        return new GarminConnectionResponse(status, "PERSONNEL", null, null, null, challengeId);
    }
}

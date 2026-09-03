package fr.pace.common;

import fr.pace.goal.GoalNotFoundException;
import fr.pace.activity.SortieNotFoundException;
import fr.pace.garmin.GarminConnectionExpiredException;
import fr.pace.garmin.GarminConnectorException;
import fr.pace.garmin.PermanentGarminConnectorException;
import fr.pace.garmin.TemporaryGarminConnectorException;
import fr.pace.garmin.synchronization.SynchronizationCandidateNotFoundException;
import fr.pace.garmin.synchronization.SynchronizationNotFoundException;
import fr.pace.profile.ProfileNotConfiguredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({ProfileNotConfiguredException.class, GoalNotFoundException.class, SortieNotFoundException.class,
            SynchronizationNotFoundException.class, SynchronizationCandidateNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Ressource introuvable");
        return detail;
    }

    @ExceptionHandler(TemporaryGarminConnectorException.class)
    public org.springframework.http.ResponseEntity<GarminErrorResponse> handleTemporaryGarmin(
            TemporaryGarminConnectorException exception
    ) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new GarminErrorResponse("GARMIN_TEMPORAIRE", exception.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler({PermanentGarminConnectorException.class, GarminConnectionExpiredException.class})
    public org.springframework.http.ResponseEntity<GarminErrorResponse> handlePermanentGarmin(
            GarminConnectorException exception
    ) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new GarminErrorResponse("GARMIN_ACTION_REQUISE", exception.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Certaines informations saisies sont invalides."
        );
        detail.setTitle("Saisie invalide");
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Le format d'une information saisie n'est pas reconnu."
        );
        detail.setTitle("Saisie illisible");
        return detail;
    }

    public record GarminErrorResponse(String code, String message, String identifiantDiagnostic) { }
}

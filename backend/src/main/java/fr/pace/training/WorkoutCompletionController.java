package fr.pace.training;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seances")
public class WorkoutCompletionController {
    private final WorkoutCompletionService service;
    public WorkoutCompletionController(WorkoutCompletionService service) { this.service = service; }

    @GetMapping("/{id}/realisation")
    public WorkoutCompletionResponse get(@PathVariable UUID id) {
        return service.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable."));
    }
}

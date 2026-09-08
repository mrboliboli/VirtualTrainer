package fr.pace.ai.api;

import fr.pace.ai.application.NextWorkoutService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seances/prochaine")
public class NextWorkoutController {
    private final NextWorkoutService service;
    public NextWorkoutController(NextWorkoutService service) { this.service = service; }
    @GetMapping public NextWorkoutResponse get() { return service.next(); }
    @PostMapping("/generation") @ResponseStatus(HttpStatus.CREATED)
    public NextWorkoutResponse generate() { return service.generate(); }
}

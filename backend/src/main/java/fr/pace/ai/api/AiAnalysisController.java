package fr.pace.ai.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.ai.application.AiAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sorties/{id}/analyse")
public class AiAnalysisController {
    private final AiAnalysisService service;
    private final ObjectMapper mapper;
    public AiAnalysisController(AiAnalysisService service, ObjectMapper mapper) { this.service = service; this.mapper = mapper; }

    @GetMapping public AiAnalysisResponse get(@PathVariable UUID id) { return AiAnalysisResponse.from(service.latest(id), mapper); }
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public AiAnalysisResponse start(@PathVariable UUID id) { return AiAnalysisResponse.from(service.start(id, false), mapper); }
    @PostMapping("/regeneration") @ResponseStatus(HttpStatus.ACCEPTED)
    public AiAnalysisResponse regenerate(@PathVariable UUID id) { return AiAnalysisResponse.from(service.start(id, true), mapper); }
}

package fr.pace.activity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/sorties")
public class SortieController {
    private final SortieReadService service;
    private final SortieDetailService detailService;

    public SortieController(SortieReadService service, SortieDetailService detailService) {
        this.service = service;
        this.detailService = detailService;
    }

    @GetMapping
    public List<SortieResponse> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limite
    ) {
        return service.recent(limite);
    }

    @GetMapping("/{id}")
    public SortieDetailResponse get(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id) {
        return detailService.get(id);
    }
}

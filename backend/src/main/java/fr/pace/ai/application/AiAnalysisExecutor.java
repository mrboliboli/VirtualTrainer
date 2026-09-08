package fr.pace.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.ai.domain.ActivityAnalysisRequest;
import fr.pace.ai.domain.AiInvalidResponseException;
import fr.pace.ai.domain.AiProvider;
import fr.pace.ai.domain.AiTemporaryException;
import fr.pace.ai.persistence.AiCall;
import fr.pace.ai.persistence.AiCallRepository;
import fr.pace.ai.persistence.AiCallStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class AiAnalysisExecutor {
    private static final List<Duration> RETRIES = List.of(Duration.ofMinutes(5), Duration.ofMinutes(30));
    private final AiCallRepository repository;
    private final AiProvider provider;
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public AiAnalysisExecutor(AiCallRepository repository, AiProvider provider, ObjectMapper mapper) {
        this.repository = repository; this.provider = provider; this.mapper = mapper;
    }

    @Async
    public void execute(UUID id) { executeNow(id); }

    void executeNow(UUID id) {
        AiCall call = repository.findById(id).orElse(null);
        if (call == null) return;
        call.start(clock.instant()); repository.save(call);
        try {
            ActivityAnalysisRequest request = mapper.readValue(call.getInputJson(), ActivityAnalysisRequest.class);
            var result = provider.analyzeActivity(request);
            call.succeed(mapper.writeValueAsString(result), null, 0, null, null, clock.instant());
        } catch (AiTemporaryException exception) {
            int index = call.getAttemptCount() - 1;
            if (index < RETRIES.size()) call.failTemporarily(exception.getMessage(), clock.instant().plus(RETRIES.get(index)), clock.instant());
            else call.failPermanently("Le service IA reste indisponible après plusieurs tentatives.", clock.instant());
        } catch (AiInvalidResponseException exception) {
            call.failPermanently("La réponse IA reçue n'est pas exploitable.", clock.instant());
        } catch (Exception exception) {
            call.failPermanently("L'analyse IA n'a pas pu être générée.", clock.instant());
        }
        repository.save(call);
    }

    @Scheduled(fixedDelayString = "${pace.ai.retry-scan-delay:60000}")
    public void retryDue() {
        var now = clock.instant();
        repository.findAllByStatusInOrderByCreatedAtAsc(List.of(AiCallStatus.ERREUR_TEMPORAIRE)).stream()
                .filter(value -> value.getNextAttemptAt() != null && !value.getNextAttemptAt().isAfter(now))
                .limit(10).forEach(value -> execute(value.getId()));
    }
}

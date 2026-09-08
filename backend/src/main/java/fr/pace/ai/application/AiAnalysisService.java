package fr.pace.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.SortieDetailService;
import fr.pace.ai.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

@Service
public class AiAnalysisService {
    private static final String PROMPT_VERSION = "activity-v1";
    private static final String SCHEMA_VERSION = "activity-analysis-v1";
    private final AiCallRepository calls;
    private final AiSettingsService settings;
    private final SortieDetailService sorties;
    private final ActivityAnalysisRequestFactory requests;
    private final AiAnalysisExecutor executor;
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public AiAnalysisService(AiCallRepository calls, AiSettingsService settings, SortieDetailService sorties,
                             ActivityAnalysisRequestFactory requests, AiAnalysisExecutor executor, ObjectMapper mapper) {
        this.calls = calls; this.settings = settings; this.sorties = sorties;
        this.requests = requests; this.executor = executor; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AiCall latest(UUID activityId) {
        return calls.findFirstByActivityIdAndOperationTypeOrderByAnalysisVersionDesc(activityId, AiOperationType.ANALYSE_ACTIVITE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune analyse IA pour cette sortie."));
    }

    @Transactional
    public AiCall start(UUID activityId, boolean regenerate) {
        var previous = calls.findFirstByActivityIdAndOperationTypeOrderByAnalysisVersionDesc(activityId, AiOperationType.ANALYSE_ACTIVITE);
        if (previous.isPresent() && (previous.get().getStatus() == AiCallStatus.EN_ATTENTE
                || previous.get().getStatus() == AiCallStatus.EN_COURS
                || previous.get().getStatus() == AiCallStatus.ERREUR_TEMPORAIRE)) return previous.get();
        if (!regenerate && previous.isPresent() && previous.get().getStatus() == AiCallStatus.REUSSIE) return previous.get();
        var configuration = settings.getOrCreate();
        if (!configuration.isEnabled()) throw new ResponseStatusException(HttpStatus.CONFLICT, "L'IA est désactivée dans les réglages.");
        if (!settings.keyConfigured()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La clé OpenAI n'est pas configurée.");
        try {
            String input = mapper.writeValueAsString(requests.create(sorties.get(activityId)));
            int version = previous.map(value -> value.getAnalysisVersion() + 1).orElse(1);
            AiCall call = calls.save(AiCall.pending(activityId, AiOperationType.ANALYSE_ACTIVITE,
                    configuration.getActiveProvider(), configuration.getAnalysisModel(), PROMPT_VERSION,
                    SCHEMA_VERSION, version, input, clock.instant()));
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { executor.execute(call.getId()); }
                });
            } else {
                executor.execute(call.getId());
            }
            return call;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les données de la sortie ne permettent pas une analyse.");
        }
    }
}

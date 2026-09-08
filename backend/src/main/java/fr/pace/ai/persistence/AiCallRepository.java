package fr.pace.ai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiCallRepository extends JpaRepository<AiCall, UUID> {
    Optional<AiCall> findFirstByActivityIdAndOperationTypeOrderByAnalysisVersionDesc(UUID activityId, AiOperationType operationType);
    List<AiCall> findAllByStatusInOrderByCreatedAtAsc(List<AiCallStatus> statuses);
}

package fr.pace.activity;

import java.time.Instant;
import java.util.List;

/** Source métier d'activités, indépendante de tout fournisseur. */
public interface ActivitySource {
    ActivitySourceStatus connectionStatus();
    List<ExternalActivitySummary> findRecentActivities(Instant since);
    ExternalActivityDetails getActivity(String sourceActivityId);
    byte[] getOriginalActivityFile(String sourceActivityId);
}

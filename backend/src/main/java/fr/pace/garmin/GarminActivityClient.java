package fr.pace.garmin;

import fr.pace.activity.ActivitySourceStatus;
import fr.pace.activity.ExternalActivityDetails;
import fr.pace.activity.ExternalActivitySummary;

import java.time.LocalDate;
import java.util.List;

/** Port d'infrastructure de l'API Garmin officielle. */
public interface GarminActivityClient {
    GarminSession connect(String email, String password, String correlationId);
    GarminSession completeMfa(String code, String correlationId);
    GarminSession sessionStatus(String correlationId);
    ActivitySourceStatus connectionStatus();
    List<ExternalActivitySummary> pullActivities(LocalDate from, LocalDate to, int limit, String correlationId);
    ExternalActivityDetails getActivity(String garminActivityId);
    byte[] downloadFit(String garminActivityId);
    void disconnect();
}

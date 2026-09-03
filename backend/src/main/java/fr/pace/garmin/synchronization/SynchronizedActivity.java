package fr.pace.garmin.synchronization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity")
public class SynchronizedActivity {
    @Id private UUID id;
    @Column(nullable = false, length = 30) private String source;
    @Column(name = "source_external_id", nullable = false, length = 128) private String sourceExternalId;
    @Column(name = "discovered_at", nullable = false) private Instant discoveredAt;
    @Column(name = "last_synchronized_at", nullable = false) private Instant lastSynchronizedAt;
    @Column(name = "processing_status", nullable = false, length = 30) private String processingStatus;
    @Column(name = "details_json", nullable = false, columnDefinition = "TEXT") private String detailsJson;
    @Column(name = "original_fit", columnDefinition = "bytea") private byte[] originalFit;
    @Column(name = "fit_fingerprint", length = 64) private String fitFingerprint;

    protected SynchronizedActivity() { }

    public static SynchronizedActivity create(
            String externalId,
            String detailsJson,
            byte[] fit,
            String fingerprint,
            Instant now
    ) {
        SynchronizedActivity activity = new SynchronizedActivity();
        activity.id = UUID.randomUUID();
        activity.source = "GARMIN_PERSONNEL";
        activity.sourceExternalId = externalId;
        activity.discoveredAt = now;
        activity.lastSynchronizedAt = now;
        activity.processingStatus = "PRETE_A_ANALYSER";
        activity.detailsJson = detailsJson;
        activity.originalFit = fit.clone();
        activity.fitFingerprint = fingerprint;
        return activity;
    }

    public UUID getId() { return id; }
    public String getSource() { return source; }
    public String getDetailsJson() { return detailsJson; }
}

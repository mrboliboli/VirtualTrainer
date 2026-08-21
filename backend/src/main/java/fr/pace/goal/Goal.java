package fr.pace.goal;

import fr.pace.profile.AthleteProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goal")
public class Goal {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_profile_id")
    private AthleteProfile athleteProfile;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "event_date", nullable = false) private LocalDate eventDate;
    @Column(precision = 8, scale = 3) private BigDecimal distance;
    @Enumerated(EnumType.STRING) @Column(name = "distance_unit", length = 20) private DistanceUnit distanceUnit;
    @Enumerated(EnumType.STRING) @Column(name = "goal_type", nullable = false, length = 30) private GoalType type;
    @Column(name = "official_url", length = 2048) private String officialUrl;
    @Column(nullable = false) private int priority;
    @Column(name = "target_time_seconds") private Integer targetTimeSeconds;
    @Column(name = "target_elevation_gain_meters") private Integer targetElevationGainMeters;
    @Column(length = 4000) private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private GoalStatus status;
    @Column(name = "primary_goal", nullable = false) private boolean primary;
    @Column(nullable = false) private boolean archived;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Goal() { }

    public static Goal create(UUID id, AthleteProfile profile, GoalData data, Instant now) {
        Goal goal = new Goal();
        goal.id = id;
        goal.athleteProfile = profile;
        goal.createdAt = now;
        goal.update(data, now);
        return goal;
    }

    public void update(GoalData data, Instant now) {
        name = data.name(); eventDate = data.eventDate(); distance = data.distance(); distanceUnit = data.distanceUnit();
        type = data.type(); officialUrl = data.officialUrl(); priority = data.priority();
        targetTimeSeconds = data.targetTimeSeconds(); targetElevationGainMeters = data.targetElevationGainMeters();
        notes = data.notes(); status = data.status(); primary = data.primary(); updatedAt = now;
    }

    public void archive(Instant now) { archived = true; primary = false; updatedAt = now; }
    public void removePrimary() { primary = false; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public LocalDate getEventDate() { return eventDate; }
    public BigDecimal getDistance() { return distance; }
    public DistanceUnit getDistanceUnit() { return distanceUnit; }
    public GoalType getType() { return type; }
    public String getOfficialUrl() { return officialUrl; }
    public int getPriority() { return priority; }
    public Integer getTargetTimeSeconds() { return targetTimeSeconds; }
    public Integer getTargetElevationGainMeters() { return targetElevationGainMeters; }
    public String getNotes() { return notes; }
    public GoalStatus getStatus() { return status; }
    public boolean isPrimary() { return primary; }
    public boolean isArchived() { return archived; }
}

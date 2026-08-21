package fr.pace.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "athlete_profile")
public class AthleteProfile {

    @Id
    private UUID id;
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Column(name = "birth_year")
    private Integer birthYear;
    @Column(name = "height_cm")
    private Integer heightCm;
    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;
    @Column(name = "maximum_heart_rate")
    private Integer maximumHeartRate;
    @Column(name = "resting_heart_rate")
    private Integer restingHeartRate;
    @Column(name = "heart_rate_threshold")
    private Integer heartRateThreshold;
    @Column(name = "power_threshold")
    private Integer powerThreshold;
    @Column(name = "usual_weekly_volume_km", precision = 6, scale = 2)
    private BigDecimal usualWeeklyVolumeKm;
    @Column(name = "available_days", length = 1000)
    private String availableDays;
    @Column(name = "maximum_session_duration_minutes")
    private Integer maximumSessionDurationMinutes;
    @Column(name = "available_terrains", length = 1000)
    private String availableTerrains;
    @Column(name = "constraints_and_injuries", length = 2000)
    private String constraintsAndInjuries;
    @Column(name = "training_preferences", length = 2000)
    private String trainingPreferences;
    @Column(length = 4000)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AthleteProfile() {
    }

    public static AthleteProfile create(UUID id, AthleteProfileData data, Instant now) {
        AthleteProfile profile = new AthleteProfile();
        profile.id = id;
        profile.createdAt = now;
        profile.update(data, now);
        return profile;
    }

    public void update(AthleteProfileData data, Instant now) {
        firstName = data.firstName();
        birthYear = data.birthYear();
        heightCm = data.heightCm();
        weightKg = data.weightKg();
        maximumHeartRate = data.maximumHeartRate();
        restingHeartRate = data.restingHeartRate();
        heartRateThreshold = data.heartRateThreshold();
        powerThreshold = data.powerThreshold();
        usualWeeklyVolumeKm = data.usualWeeklyVolumeKm();
        availableDays = data.availableDays();
        maximumSessionDurationMinutes = data.maximumSessionDurationMinutes();
        availableTerrains = data.availableTerrains();
        constraintsAndInjuries = data.constraintsAndInjuries();
        trainingPreferences = data.trainingPreferences();
        notes = data.notes();
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public Integer getBirthYear() { return birthYear; }
    public Integer getHeightCm() { return heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public Integer getMaximumHeartRate() { return maximumHeartRate; }
    public Integer getRestingHeartRate() { return restingHeartRate; }
    public Integer getHeartRateThreshold() { return heartRateThreshold; }
    public Integer getPowerThreshold() { return powerThreshold; }
    public BigDecimal getUsualWeeklyVolumeKm() { return usualWeeklyVolumeKm; }
    public String getAvailableDays() { return availableDays; }
    public Integer getMaximumSessionDurationMinutes() { return maximumSessionDurationMinutes; }
    public String getAvailableTerrains() { return availableTerrains; }
    public String getConstraintsAndInjuries() { return constraintsAndInjuries; }
    public String getTrainingPreferences() { return trainingPreferences; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

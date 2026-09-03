package fr.pace.goal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GoalStatus {
    PLANNED("PREVU"),
    ACTIVE("ACTIF"),
    ACHIEVED("ATTEINT"),
    ABANDONED("ABANDONNE");

    private final String jsonValue;

    GoalStatus(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static GoalStatus fromJson(String value) {
        for (GoalStatus status : values()) {
            if (status.jsonValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Statut d'objectif inconnu : %s".formatted(value));
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }
}

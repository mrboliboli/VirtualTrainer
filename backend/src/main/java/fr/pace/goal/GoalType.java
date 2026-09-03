package fr.pace.goal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GoalType {
    RUNNING("RUNNING"),
    TRAIL("TRAIL"),
    ROUTE("ROUTE"),
    FIVE_KILOMETERS("5_KM"),
    TEN_KILOMETERS("10_KM"),
    HALF_MARATHON("SEMI_MARATHON"),
    MARATHON("MARATHON"),
    OTHER("AUTRE");

    private final String jsonValue;

    GoalType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static GoalType fromJson(String value) {
        for (GoalType type : values()) {
            if (type.jsonValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type d'objectif inconnu : %s".formatted(value));
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }
}

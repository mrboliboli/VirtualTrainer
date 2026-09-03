package fr.pace.goal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DistanceUnit {
    KILOMETER("KM"),
    MILE("MILES");

    private final String jsonValue;

    DistanceUnit(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static DistanceUnit fromJson(String value) {
        for (DistanceUnit unit : values()) {
            if (unit.jsonValue.equals(value)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unité de distance inconnue : %s".formatted(value));
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }
}

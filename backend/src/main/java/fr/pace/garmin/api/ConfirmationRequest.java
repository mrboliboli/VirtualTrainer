package fr.pace.garmin.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmationRequest(@NotBlank @Size(max = 128) String idExterne) {
}

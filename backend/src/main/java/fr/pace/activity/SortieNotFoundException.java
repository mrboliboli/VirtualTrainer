package fr.pace.activity;

import java.util.UUID;

public class SortieNotFoundException extends RuntimeException {
    public SortieNotFoundException(UUID id) { super("La sortie %s est introuvable.".formatted(id)); }
}

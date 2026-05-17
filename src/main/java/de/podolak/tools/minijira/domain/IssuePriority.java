package de.podolak.tools.minijira.domain;

import java.util.Locale;

public enum IssuePriority {
    VERY_HIGH("Sehr hoch"),
    HIGH("Hoch"),
    MEDIUM("Mittel"),
    LOW("Niedrig"),
    VERY_LOW("Sehr niedrig");

    private final String label;

    IssuePriority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static IssuePriority fromRequestValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Priority is required");
        }
        try {
            return IssuePriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported issue priority: " + value, e);
        }
    }
}

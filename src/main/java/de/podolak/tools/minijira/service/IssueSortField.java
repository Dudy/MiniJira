package de.podolak.tools.minijira.service;

public enum IssueSortField {
    ID,
    AUTHOR,
    PRIORITY;

    public static IssueSortField fromRequestValue(String value) {
        if (value == null || value.isBlank()) {
            return ID;
        }
        return switch (value.trim().toLowerCase()) {
            case "id" -> ID;
            case "author", "autor" -> AUTHOR;
            case "priority", "prioritaet", "priorität" -> PRIORITY;
            default -> throw new BadRequestException("Unsupported issue sort field: " + value);
        };
    }
}

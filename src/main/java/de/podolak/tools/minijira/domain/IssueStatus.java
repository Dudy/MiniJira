package de.podolak.tools.minijira.domain;

import java.util.Locale;

public enum IssueStatus {
    TODO("to do"),
    DOING("doing"),
    TESTING("testing"),
    REVIEWING("reviewing"),
    DONE("done");

    private final String label;

    IssueStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static IssueStatus fromRequestValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Status is required");
        }
        try {
            return IssueStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported issue status: " + value, e);
        }
    }
}

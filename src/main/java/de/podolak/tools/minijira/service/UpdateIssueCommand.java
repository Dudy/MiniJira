package de.podolak.tools.minijira.service;

import java.util.List;

public record UpdateIssueCommand(
        List<Integer> workerUserIds,
        String title,
        String description,
        String priority,
        String status
) {
}

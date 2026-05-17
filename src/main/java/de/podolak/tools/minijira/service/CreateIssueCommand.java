package de.podolak.tools.minijira.service;

import java.util.List;

public record CreateIssueCommand(
        Integer authorUserId,
        List<Integer> workerUserIds,
        String title,
        String description,
        String priority,
        String status
) {
}

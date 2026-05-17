package de.podolak.tools.minijira.repo;

import de.podolak.tools.minijira.domain.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, Integer> {
}

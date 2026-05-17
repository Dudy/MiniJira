package de.podolak.tools.minijira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "issue")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "issue_worker",
            joinColumns = @JoinColumn(name = "issue_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @OrderBy("id ASC")
    private Set<AppUser> workers = new LinkedHashSet<>();

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Integer priority;

    protected Issue() {
    }

    public Issue(AppUser author, Set<AppUser> workers, String title, String description, Integer priority) {
        this.author = author;
        this.workers = workers;
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    public Integer getId() {
        return id;
    }

    public AppUser getAuthor() {
        return author;
    }

    public Set<AppUser> getWorkers() {
        return workers;
    }

    public void setWorkers(Set<AppUser> workers) {
        this.workers = workers;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}

package org.verapdf.crawler.logius.model;

import org.verapdf.crawler.logius.crawling.CrawlJob;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "client")
public class User {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Column
    private boolean enabled;

    @Column
    private boolean activated;

    @Column
    private byte[] secret;

    @Column
    @Enumerated(EnumType.STRING)
    private Role role;
    @OneToMany
    @JoinColumn(name = "user_id")
    private List<CrawlJob> crawlJobs;

    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.enabled = true;
    }

    public List<CrawlJob> getCrawlJobs() {
        return crawlJobs;
    }

    public void setCrawlJobs(List<CrawlJob> crawlJobs) {
        this.crawlJobs = crawlJobs;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public byte[] getSecret() {
        return secret;
    }

    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }
}

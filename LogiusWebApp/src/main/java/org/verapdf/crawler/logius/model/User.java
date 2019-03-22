package org.verapdf.crawler.logius.model;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "client")
public class User {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false )
    private UUID id;
    @Column(unique = true)
    private String email;
    @Column
    private String password;
    @Column
    private boolean enabled;
    @Column
    private byte[] secret;
    @Column
    @Enumerated(EnumType.STRING)
    private Roles role;

    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.enabled = true;
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

    public Roles getRole() {
        return role;
    }

    public void setRole(Roles role) {
        this.role = role;
    }


    public enum Roles{
        ADMIN, USER
    }
}

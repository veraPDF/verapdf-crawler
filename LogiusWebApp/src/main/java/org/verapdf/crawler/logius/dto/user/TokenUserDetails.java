package org.verapdf.crawler.logius.dto.user;


import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.verapdf.crawler.logius.model.Role;

import java.util.Collections;
import java.util.UUID;

public class TokenUserDetails extends User {
    private String token;
    private UUID uuid;

    public TokenUserDetails(String username, String password, boolean enabled, Role role, String token) {
        super(username, password, enabled, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority(role.toString())));
        this.token = token;
    }

    public TokenUserDetails(UUID id, String username, String password, boolean enabled, Role role, String token) {
        super(username, password, enabled, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority(role.toString())));
        this.token = token;
        this.uuid = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getToken() {
        return token;
    }
}
package org.verapdf.crawler.logius.dto.user;


import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.verapdf.crawler.logius.model.Role;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TokenUserDetails extends User {
    private String token;
    private UUID uuid;

    public TokenUserDetails(UUID id, String username, String password, boolean enabled, boolean isActivated, String token, Role role, String... scopes) {
        super(username, password, enabled, true, true, isActivated, mergeAuthorities(role, scopes));
        this.token = token;
        this.uuid = id;

    }

    public UUID getUuid() {
        return uuid;
    }

    public String getToken() {
        return token;
    }

    private static List<SimpleGrantedAuthority> mergeAuthorities(Role role, String[] scopes) {
        List<SimpleGrantedAuthority> authorities = Stream.of(scopes).map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority(role.toString()));
        return authorities;
    }
}
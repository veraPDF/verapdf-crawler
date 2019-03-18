package org.verapdf.crawler.logius.dto;

import org.springframework.security.core.GrantedAuthority;
import org.verapdf.crawler.logius.exception.NotFoundException;
import org.verapdf.crawler.logius.model.User;

import javax.validation.constraints.Email;
import java.util.Set;
import java.util.stream.Collectors;

public class UserInfoDto {
    @Email
    private String email;
    private boolean isEnabled;
    private Set<String> roles;

    public UserInfoDto(User user) {
        this.email = user.getEmail();
        this.isEnabled = user.isEnabled();
    }

    public UserInfoDto(TokenUserDetails userPrincipal) {
        this.roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        this.email = userPrincipal.getUsername();
        this.isEnabled = userPrincipal.isEnabled();
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}

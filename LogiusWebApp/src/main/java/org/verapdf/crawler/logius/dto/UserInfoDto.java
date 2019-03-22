package org.verapdf.crawler.logius.dto;

import org.verapdf.crawler.logius.model.User;

public class UserInfoDto {
    private String email;
    private String role;
    private boolean isEnabled;

    public UserInfoDto(User user) {
        this.email = user.getEmail();
        this.isEnabled = user.isEnabled();
    }

    public UserInfoDto(TokenUserDetails userPrincipal) {
        this.role = userPrincipal.getAuthorities().iterator().next().getAuthority();
        this.email = userPrincipal.getUsername();
        this.isEnabled = userPrincipal.isEnabled();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

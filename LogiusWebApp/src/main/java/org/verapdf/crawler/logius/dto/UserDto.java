package org.verapdf.crawler.logius.dto;



import org.verapdf.crawler.logius.model.User;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class UserDto {
    @Email
    @NotNull
    private String email;
    @Pattern(regexp = "(^[0-9a-zA-Z]{8,20}$)", message = "password must be between 8 and 20 characters and include 0-9 a-z A-Z symbols")
    @NotNull
    private String password;

    public UserDto() {
    }

    public UserDto(User user) {
        this.email = user.getEmail();
        this.password = user.getPassword();
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
}

package org.verapdf.crawler.logius.dto.user;

import org.verapdf.crawler.logius.tools.Constants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class PasswordResetDto {

    @Pattern(regexp = Constants.PASSWORD_PATTERN, message = "Passwords must be 6 or more characters, " +
            "contain a combination of uppercase and lowercase letters (A-Z or a-z), " +
            "at least 1 number (0-9) and at least 1 special character(#$^ etc).")
    @NotNull
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

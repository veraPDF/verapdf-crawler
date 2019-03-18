package org.verapdf.crawler.logius.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class PasswordUpdateDto {
    @Pattern(regexp = "(^[0-9a-zA-Z]{8,20}$)", message = "old password must be between 8 and 20 characters and include 0-9 a-z A-Z symbols")
    @NotNull
    private String oldPassword;
    @Pattern(regexp = "(^[0-9a-zA-Z]{8,20}$)", message = "new password must be between 8 and 20 characters and include 0-9 a-z A-Z symbols")
    @NotNull
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

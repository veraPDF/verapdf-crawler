package org.verapdf.crawler.logius.resources;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.dto.TokenDto;
import org.verapdf.crawler.logius.dto.user.*;
import org.verapdf.crawler.logius.service.UserService;

import javax.validation.Valid;
import javax.validation.constraints.Email;


@RestController
@RequestMapping("api/user")
public class UserResource {
    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/me")
    @PreAuthorize("isFullyAuthenticated()")
    public UserInfoDto getUserInfo(@AuthenticationPrincipal TokenUserDetails principal) {
        return new UserInfoDto(principal);
    }

    @PutMapping("/password")
    @PreAuthorize("isFullyAuthenticated()")
    public ResponseEntity updatePassword(@AuthenticationPrincipal TokenUserDetails principal,
                                         @Valid @RequestBody PasswordUpdateDto passwordUpdateDto) {
        userService.updatePassword(principal.getUsername(), passwordUpdateDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{email}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity updateStatus(@PathVariable("email") @Email String email,
                                       @RequestParam("status") boolean status) {
        userService.updateStatus(email, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{email}/verification-status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity updateVerificationStatus(@PathVariable("email") @Email String email,
                                       @RequestParam("status") boolean status) {
        userService.updateEmailVerificationStatus(email, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity getAllUsers(
            @RequestParam(value = "emailFilter", required = false) String emailFilter,
            @RequestParam("start") int startParam,
            @RequestParam("limit") int limitParam) {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(userService.count(emailFilter)))
                .body(userService.getUsers(emailFilter, startParam, limitParam));

    }

    @PostMapping("/signup")
    public ResponseEntity registerUser(@RequestBody @Valid UserDto signUpRequest) {
        userService.registerUser(signUpRequest);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/email-confirm")
    public TokenDto emailConfirm(@AuthenticationPrincipal TokenUserDetails principal) {
        return new TokenDto(userService.confirmUserEmail(principal.getToken()));
    }

    @PostMapping("/email-resend")
    public ResponseEntity emailResend(@Email @RequestParam(value = "email") String email) {
        userService.resendVerificationEmail(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset")
    public void resetPassword(@Email @RequestParam(value = "email") String email) {
        userService.resetPassword(email);
    }

    @PostMapping("/password-reset-confirm")
    public void passwordResetConfirm(@AuthenticationPrincipal TokenUserDetails principal,
                                               @RequestBody @Valid PasswordResetDto passwordResetDto){
        userService.confirmResetPassword(principal.getUuid(), passwordResetDto.getNewPassword());
    }
}
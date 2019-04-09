package org.verapdf.crawler.logius.resources;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.core.email.SendEmail;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.dto.ApiErrorDto;
import org.verapdf.crawler.logius.dto.user.*;
import org.verapdf.crawler.logius.model.User;
import org.verapdf.crawler.logius.service.TokenService;
import org.verapdf.crawler.logius.service.UserService;

import javax.validation.Valid;
import javax.validation.constraints.Email;


@RestController
@RequestMapping("api/user")
public class UserResource {
    private final UserService userService;
    private final CrawlJobDAO crawlJobDAO;
    private final TokenService tokenService;
    private final SendEmail sendEmail;

    public UserResource(UserService userService, CrawlJobDAO crawlJobDAO, TokenService tokenService, SendEmail sendEmail) {
        this.userService = userService;
        this.crawlJobDAO = crawlJobDAO;
        this.tokenService = tokenService;
        this.sendEmail = sendEmail;
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
        User user = userService.save(signUpRequest);
        sendEmail.sendEmailConfirm(tokenService.encode(user), user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/email-confirm")
    public String emailConfirm(@RequestParam(value = "token") String token) {
        return userService.confirmUserEmail(token);
    }

    @PostMapping("/{email}/email-resend")
    public ResponseEntity emailResend(@Email @PathVariable(value = "email") String email) {
        User user = userService.findUserByEmail(email);
        if (user.isActivated()){
            return ResponseEntity.badRequest().body(new ApiErrorDto("user already activated"));
        }
        sendEmail.sendEmailConfirm(tokenService.encode(user), email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{email}/password-reset")
    public void resetPassword(@Email @PathVariable(value = "email") String email) {
        User user = userService.findUserByEmail(email);
        sendEmail.sendPasswordResetToken(tokenService.encodePasswordToken(user), user.getEmail());
    }

    @PreAuthorize("hasAuthority('RESET_PASSWORD')")
    @PostMapping("/password-reset-confirm")
    public void passwordResetConfirm(@AuthenticationPrincipal TokenUserDetails principal,
                                               @RequestBody @Valid PasswordResetDto passwordResetDto){
        userService.confirmPasswordReset(principal.getUuid(), passwordResetDto.getNewPassword());
    }
}
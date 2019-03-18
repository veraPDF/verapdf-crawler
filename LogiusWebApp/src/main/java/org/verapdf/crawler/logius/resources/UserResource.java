package org.verapdf.crawler.logius.resources;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.dto.PasswordUpdateDto;
import org.verapdf.crawler.logius.dto.TokenUserDetails;
import org.verapdf.crawler.logius.dto.UserInfoDto;
import org.verapdf.crawler.logius.service.UserService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;


@RestController
@RequestMapping("api/user")
public class UserResource {
    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/check")
    public UserInfoDto getUserInfo() {
        while(true){
            try {
                Thread.sleep(1000);
                System.out.println(123);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/current")
    @PreAuthorize(value = "isFullyAuthenticated()")
    public UserInfoDto getUserInfo(@AuthenticationPrincipal TokenUserDetails principal) {
        return new UserInfoDto(principal);
    }

    @PutMapping(value = "/update-password")
    @PreAuthorize(value = "isFullyAuthenticated()")
    public ResponseEntity updatePassword(@AuthenticationPrincipal TokenUserDetails principal,
                                         @Valid @NotNull @RequestBody PasswordUpdateDto passwordUpdateDto) {
        userService.updatePassword(principal.getUsername(), passwordUpdateDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/update-status")
    @PreAuthorize(value = "hasAuthority('ADMIN')")
    public ResponseEntity updateStatus(@RequestParam("email") String email,
                                       @RequestParam("status") boolean status) {
        userService.updateStatus(email, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize(value = "hasAuthority('ADMIN')")
    public ResponseEntity getAllUsers(
            @RequestParam(value = "emailFilter", required = false) String emailFilter,
            @RequestParam("start") int startParam,
            @RequestParam("limit") int limitParam) {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(userService.count(emailFilter)))
                .body(userService.getUsers(emailFilter, startParam, limitParam));

    }
}
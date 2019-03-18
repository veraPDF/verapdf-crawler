package org.verapdf.crawler.logius.resources;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.dto.TokenUserDetails;
import org.verapdf.crawler.logius.dto.UserDto;
import org.verapdf.crawler.logius.dto.UserInfoDto;
import org.verapdf.crawler.logius.service.UserService;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/token")
    public String getToken(@AuthenticationPrincipal TokenUserDetails principal) {
        return principal.getToken();
    }


    @PostMapping("/signup")
    public ResponseEntity registerUser(@Valid @RequestBody UserDto signUpRequest) {
        UserInfoDto result = userService.save(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
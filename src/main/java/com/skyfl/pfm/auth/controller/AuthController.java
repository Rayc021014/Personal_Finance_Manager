package com.skyfl.pfm.auth.controller;

import com.skyfl.pfm.auth.dto.ChangePasswordRequest;
import com.skyfl.pfm.auth.dto.LoginRequest;
import com.skyfl.pfm.auth.dto.LogoutRequest;
import com.skyfl.pfm.auth.dto.RefreshTokenRequest;
import com.skyfl.pfm.auth.dto.RegisterRequest;
import com.skyfl.pfm.auth.dto.TokenResponse;
import com.skyfl.pfm.auth.service.AuthService;
import com.skyfl.pfm.common.security.CurrentUser;
import com.skyfl.pfm.common.security.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.accessToken(), request.refreshToken());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        CurrentUser currentUser = CurrentUserResolver.get();
        authService.changePassword(currentUser.getId(), request);
    }
}

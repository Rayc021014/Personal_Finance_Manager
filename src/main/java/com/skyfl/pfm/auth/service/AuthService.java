package com.skyfl.pfm.auth.service;

import com.skyfl.pfm.auth.dto.ChangePasswordRequest;
import com.skyfl.pfm.auth.dto.LoginRequest;
import com.skyfl.pfm.auth.dto.RegisterRequest;
import com.skyfl.pfm.auth.dto.TokenResponse;
import com.skyfl.pfm.auth.entity.RefreshToken;
import com.skyfl.pfm.auth.repository.RefreshTokenRepository;
import com.skyfl.pfm.common.exception.BusinessException;
import com.skyfl.pfm.common.util.HashUtils;
import com.skyfl.pfm.user.entity.User;
import com.skyfl.pfm.user.repository.UserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final long refreshTokenDays;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setDisplayName(request.displayName().trim());
        user.setCurrency(request.currency().trim().toUpperCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(HashUtils.sha256(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }
        stored.setRevoked(true);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        String jti = jwtService.extractJti(accessToken);
        tokenBlacklistService.blacklist(jti, Duration.ofSeconds(jwtService.getAccessTokenSeconds()));

        refreshTokenRepository.findByTokenHash(HashUtils.sha256(refreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(token -> token.setRevoked(true));
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(HashUtils.sha256(refreshToken));
        token.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenDays));
        refreshTokenRepository.save(token);

        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenSeconds());
    }
}

package com.nabra.backend.modules.usermanagement.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import com.nabra.backend.modules.usermanagement.dto.AuthDtos;
import com.nabra.backend.modules.usermanagement.exception.InvalidCredentialsException;
import com.nabra.backend.modules.usermanagement.exception.UserAlreadyExistsException;
import com.nabra.backend.modules.usermanagement.exception.UserNotFoundException;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import com.nabra.backend.security.google.GoogleTokenVerifier;
import com.nabra.backend.security.jwt.JwtService;
import com.nabra.backend.security.mail.MailService;
import com.nabra.backend.security.password.PasswordResetToken;
import com.nabra.backend.security.password.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final MailService mailService;
    private final GoogleTokenVerifier googleTokenVerifier;

    // ============================
    // REGISTER
    // ============================
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {

        if (userRepository.existsByUsername(req.username())) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        if (userRepository.existsByEmail(req.email())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setDisplayName(req.displayName());
        user.setUserType(req.userType());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);

        user = userRepository.save(user);

        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );

        return buildAuthResponse(user, token);
    }

    // ============================
    // LOGIN
    // ============================
    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.username(),
                            req.password()
                    )
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("User is not active");
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );

        return buildAuthResponse(user, token);
    }

    // ============================
    // CHANGE PASSWORD (logged in)
    // ============================
    @Transactional
    public void changePassword(String userId, AuthDtos.ChangePasswordRequest req) {

        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    // ============================
    // FORGOT PASSWORD
    // ============================
    @Transactional
    public void forgotPassword(AuthDtos.ForgotPasswordRequest req) {

        User user = userRepository.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("Email not found"));

        resetTokenRepository.deleteAllByEmail(user.getEmail());

        String code = String.format("%06d", new Random().nextInt(1_000_000));

        PasswordResetToken token = PasswordResetToken.builder()
                .email(user.getEmail())
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        resetTokenRepository.save(token);

        mailService.sendResetCode(user.getEmail(), code);
    }

    // ============================
    // VERIFY RESET CODE
    // ============================
    public void verifyResetCode(AuthDtos.VerifyResetCodeRequest req) {

        PasswordResetToken token = resetTokenRepository
                .findByEmailAndCodeAndUsedFalse(
                        req.email().trim().toLowerCase(),
                        req.code()
                )
                .orElseThrow(() -> new InvalidCredentialsException("Invalid reset code"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Reset code expired");
        }
    }

    // ============================
    // RESET PASSWORD
    // ============================
    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {

        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        PasswordResetToken token = resetTokenRepository
                .findByEmailAndCodeAndUsedFalse(
                        req.email().trim().toLowerCase(),
                        req.code()
                )
                .orElseThrow(() -> new InvalidCredentialsException("Invalid reset code"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Reset code expired");
        }

        User user = userRepository.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        token.setUsed(true);
        resetTokenRepository.save(token);
    }

    // ============================
    // GOOGLE AUTH
    // ============================
    @Transactional
    public AuthDtos.AuthResponse googleAuth(AuthDtos.GoogleAuthRequest req) {

        GoogleIdToken.Payload payload;
        try {
            payload = googleTokenVerifier.verify(req.idToken());
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid Google token");
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String googleId = payload.getSubject();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUsername(email.split("@")[0]);
            user.setDisplayName(name);
            user.setUserType(UserType.OTHER);

            user.setRole(UserRole.USER);
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.setPasswordHash(passwordEncoder.encode(googleId));

            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );

        return buildAuthResponse(user, token);
    }

    // ============================
    // HELPER
    // ============================
    private AuthDtos.AuthResponse buildAuthResponse(User user, String token) {
        return new AuthDtos.AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus(),
                user.isEmailVerified()
        );
    }
}

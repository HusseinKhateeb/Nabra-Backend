package com.nabra.backend.security.password;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByEmailAndCodeAndUsedFalse(
            String email,
            String code
    );

    void deleteAllByEmail(String email);
}

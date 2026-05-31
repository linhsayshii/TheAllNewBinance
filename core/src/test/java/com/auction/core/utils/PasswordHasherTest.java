package com.auction.core.utils;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    @DisplayName("Should hash password and successfully verify it")
    void testHashAndVerify() {
        String plainText = "mySecurePassword123";
        String hashedPassword = PasswordHasher.hash(plainText);

        assertThat(hashedPassword).isNotEqualTo(plainText);
        assertThat(PasswordHasher.verify(plainText, hashedPassword)).isTrue();
    }

    @Test
    @DisplayName("Should fail verification for incorrect password")
    void testVerify_IncorrectPassword() {
        String plainText = "mySecurePassword123";
        String hashedPassword = PasswordHasher.hash(plainText);

        assertThat(PasswordHasher.verify("wrongPassword", hashedPassword)).isFalse();
    }

    @Test
    @DisplayName("Should return false when verifying with null inputs")
    void testVerify_NullInputs() {
        assertThat(PasswordHasher.verify(null, "hash")).isFalse();
        assertThat(PasswordHasher.verify("password", null)).isFalse();
    }

    @Test
    @DisplayName("Should reject null password when hashing")
    void testHash_NullPassword() {
        assertThatThrownBy(() -> PasswordHasher.hash(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

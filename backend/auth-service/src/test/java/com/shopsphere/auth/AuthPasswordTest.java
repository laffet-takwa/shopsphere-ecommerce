package com.shopsphere.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthPasswordTest {
    @Test
    void bcryptStoresOnlyAHashAndVerifiesThePassword() {
        var encoder = new BCryptPasswordEncoder();
        String rawPassword = "correct horse battery staple";
        String hash = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, hash);
        assertTrue(encoder.matches(rawPassword, hash));
    }
}
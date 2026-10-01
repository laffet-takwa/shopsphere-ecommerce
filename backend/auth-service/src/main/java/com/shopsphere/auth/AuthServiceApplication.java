package com.shopsphere.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.shopsphere.auth.user.UserAccount;
import com.shopsphere.auth.user.UserRepository;
import com.shopsphere.auth.user.UserRole;

@SpringBootApplication
public class AuthServiceApplication {
    @Bean
    ApplicationRunner bootstrapAdmin(UserRepository users, PasswordEncoder passwords,
                                     @Value("${app.bootstrap-admin.email}") String email,
                                     @Value("${app.bootstrap-admin.password}") String password) {
        return args -> {
            if (email.isBlank() && password.isBlank()) return;
            if (email.isBlank() || password.length() < 12) {
                throw new IllegalStateException("Configure both bootstrap admin email and a password of at least 12 characters");
            }
            if (!users.existsByEmailIgnoreCase(email)) {
                users.save(new UserAccount("ShopSphere", "Admin", email.trim().toLowerCase(),
                        passwords.encode(password), UserRole.ADMIN));
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
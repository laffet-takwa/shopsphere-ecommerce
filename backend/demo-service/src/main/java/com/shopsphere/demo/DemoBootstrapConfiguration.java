package com.shopsphere.demo;

import com.shopsphere.auth.user.UserAccount;
import com.shopsphere.auth.user.UserRepository;
import com.shopsphere.auth.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The bootstrap admin lives as a bean inside {@code AuthServiceApplication}, which the demo excludes
 * because that class is an entry point. The logic is reproduced here so the hosted demo can create
 * an administrator from environment variables exactly as the standalone service does.
 */
@Configuration
public class DemoBootstrapConfiguration {

    @Bean
    ApplicationRunner bootstrapAdmin(UserRepository users, PasswordEncoder passwords,
                                     @Value("${app.bootstrap-admin.email}") String email,
                                     @Value("${app.bootstrap-admin.password}") String password) {
        return args -> {
            if (email.isBlank() && password.isBlank()) {
                return;
            }
            if (email.isBlank() || password.length() < 12) {
                throw new IllegalStateException(
                        "Configure both bootstrap admin email and a password of at least 12 characters");
            }
            if (!users.existsByEmailIgnoreCase(email)) {
                users.save(new UserAccount("ShopSphere", "Admin", email.trim().toLowerCase(),
                        passwords.encode(password), UserRole.ADMIN));
            }
        };
    }
}

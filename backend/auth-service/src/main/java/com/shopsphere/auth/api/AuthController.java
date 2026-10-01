package com.shopsphere.auth.api;

import com.shopsphere.auth.security.JwtService;
import com.shopsphere.auth.user.UserAccount;
import com.shopsphere.auth.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder passwords, JwtService jwt) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        UserAccount user = users.save(new UserAccount(request.firstName().trim(), request.lastName().trim(),
                request.email().trim().toLowerCase(), passwords.encode(request.password())));
        return response(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(account -> passwords.matches(request.password(), account.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return response(user);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName()).map(AuthController::userResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private AuthResponse response(UserAccount user) {
        return new AuthResponse(jwt.createToken(user), "Bearer", userResponse(user));
    }

    private static UserResponse userResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), user.getCreatedAt());
    }

    public record RegisterRequest(@NotBlank @Size(max = 80) String firstName,
                                  @NotBlank @Size(max = 80) String lastName,
                                  @NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record AuthResponse(String accessToken, String tokenType, UserResponse user) {}
    public record UserResponse(Long id, String firstName, String lastName, String email,
                               String role, Instant createdAt) {}
}
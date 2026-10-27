package com.codekerdos.OrderServiceApplication.service;

import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.codekerdos.OrderServiceApplication.dto.RegisterRequest;
import com.codekerdos.OrderServiceApplication.entity.AppUser;
import com.codekerdos.OrderServiceApplication.enums.Role;
import com.codekerdos.OrderServiceApplication.repository.AppUserRepository;
import com.codekerdos.OrderServiceApplication.security.JwtService;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already registered");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.CUSTOMER);

        appUserRepository.save(user);
        return tokenFor(user);
    }

    public AuthResponse login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Invalid credential"));

        return tokenFor(user);
    }

    public AuthResponse tokenFor(AppUser user) {
        String token = jwtService.generateToken(
                user.getEmail(),
                Map.of("role", user.getRole().name()));

        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole());
    }

    public record AuthResponse(String token, String email, String fullName, Role role) {
    }
}

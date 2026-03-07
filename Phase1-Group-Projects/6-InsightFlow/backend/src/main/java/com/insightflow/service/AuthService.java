package com.insightflow.service;

import com.insightflow.exception.AppException;
import com.insightflow.config.JwtService;
import com.insightflow.dto.*;
import com.insightflow.model.User;
import com.insightflow.model.enums.Role;
import com.insightflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw AppException.conflict("Email already registered");
        User user = User.builder().name(request.getName()).email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())).role(Role.USER).build();
        userRepository.save(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.builder().token(token).email(user.getEmail())
                .name(user.getName()).role(user.getRole().name()).build();
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AppException.unauthorized("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw AppException.unauthorized("Invalid credentials");
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.builder().token(token).email(user.getEmail())
                .name(user.getName()).role(user.getRole().name()).build();
    }

    // Get all users
    public List<User> getAllUsers() {
//        String password00 = passwordEncoder.encode("password123");
//        String password01 = passwordEncoder.encode("password123");
//        String password02 = passwordEncoder.encode("password123");
//        List<User> users = new ArrayList<>();
//        // For demonstration, we will create some dummy users if the database is empty
//        users.add(User.builder().name("Alice").email("alice@example.com")
//                .password(password00).role(Role.USER).build());
//        users.add(User.builder().name("Bob").email("bob@example.com")
//                .password(password01).role(Role.USER).build());
//        users.add(User.builder().name("Charlie").email("charlie@example.com")
//                .password(password02).role(Role.USER).build());
//        return users;
        return userRepository.findAll();
    }
}

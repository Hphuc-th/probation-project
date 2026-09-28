package com.example.demo.service;

import com.example.demo.dto.request.auth.LoginRequest;
import com.example.demo.dto.request.auth.RegisterRequest;
import com.example.demo.dto.request.auth.RefreshTokenRequest;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.entity.Customer;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.mapper.CustomerMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String accessToken = tokenService.generateAccessToken(auth);
        String refreshToken = tokenService.generateRefreshToken(auth);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        return userMapper.toResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists: " + request.getUsername());
        }

        if (customerRepository.existsByCccd(request.getCccd())) {
            throw new BusinessException("CCCD already exists: " + request.getCccd());
        }

        Customer savedCustomer = customerRepository.save(customerMapper.toEntity(request));

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCustomer(savedCustomer);
        userRepository.save(user);
        savedCustomer.setUser(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities());
        String accessToken = tokenService.generateAccessToken(auth);
        String refreshToken = tokenService.generateRefreshToken(auth);

        return userMapper.toResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenService.isRefreshToken(refreshToken)) {
            throw new BusinessException("Invalid refresh token");
        }

        String newAccessToken = tokenService.generateAccessTokenFromRefreshToken(refreshToken);
        String newRefreshToken = tokenService.generateRefreshToken(
                new UsernamePasswordAuthenticationToken(
                        tokenService.extractUserName(refreshToken),
                        null,
                        java.util.Collections.emptyList()
                )
        );

        User user = userRepository.findByUsername(tokenService.extractUserName(refreshToken))
                .orElseThrow(() -> new BusinessException("User not found"));

        return userMapper.toResponse(user, newAccessToken, newRefreshToken);
    }
}
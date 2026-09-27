package com.example.demo.service;

import com.example.demo.dto.request.auth.LoginRequest;
import com.example.demo.dto.request.auth.RegisterRequest;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.entity.Customer;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.CustomerStatus;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenService.generateJwt(auth);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        return new AuthResponse(user.getUsername(), token);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists: " + request.getUsername());
        }

        if (customerRepository.existsByCccd(request.getCccd())) {
            throw new BusinessException("CCCD already exists: " + request.getCccd());
        }

        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setLocation(request.getLocation());
        customer.setCccd(request.getCccd());
        customer.setStatus(CustomerStatus.ACTIVE);
        Customer savedCustomer = customerRepository.save(customer);

        Role userRole = roleRepository.findByAuthority("CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role(null, "CUSTOMER")));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(roles);
        user.setCustomer(savedCustomer);
        userRepository.save(user);

        savedCustomer.setUser(user);

        String token = tokenService.generateJwt(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities())
        );

        return new AuthResponse(user.getUsername(), token);
    }
}
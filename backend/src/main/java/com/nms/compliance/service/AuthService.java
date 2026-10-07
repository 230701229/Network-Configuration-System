package com.nms.compliance.service;

import com.nms.compliance.dto.request.LoginRequest;
import com.nms.compliance.dto.response.LoginResponse;
import com.nms.compliance.entity.User;
import com.nms.compliance.repository.UserRepository;
import com.nms.compliance.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    public LoginResponse authenticate(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);
        
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresAt(new Date().getTime() + jwtExpirationMs)
                .build();
    }
}

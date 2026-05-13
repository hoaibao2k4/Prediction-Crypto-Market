package com.market.prediction.service.implement;

import java.time.Duration;
import java.util.Objects;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.market.prediction.dto.request.LoginRequest;
import com.market.prediction.dto.request.RegisterRequest;
import com.market.prediction.dto.response.AuthenticationResponse;
import com.market.prediction.entity.User;
import com.market.prediction.enums.Role;
import com.market.prediction.exception.ConflictResourceException;
import com.market.prediction.repository.UserRepository;
import com.market.prediction.security.CustomUserDetails;
import com.market.prediction.security.JwtService;
import com.market.prediction.service.AuthenticationService;
import com.market.prediction.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  private final UserRepository userRepository;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final StringRedisTemplate redisTemplate;
  private final UserService userService;
  private final PasswordEncoder passwordEncoder;

  @Override
  public AuthenticationResponse login(LoginRequest loginRequest) {
    authenticationManager
        .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    User user = userRepository.findByUsername(loginRequest.getUsername())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    CustomUserDetails customUserDetails = new CustomUserDetails(user);
    String accessToken = jwtService.generateAccessToken(customUserDetails);
    String refreshToken = jwtService.generateRefreshToken(customUserDetails);

    String redisKey = "RT:" + user.getUsername();
    redisTemplate.opsForValue().set(redisKey, Objects.requireNonNull(refreshToken), Duration.ofDays(7));

    return AuthenticationResponse.builder()
        .email(user.getEmail())
        .username(user.getUsername())
        .role(user.getRole())
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  @Override
  public AuthenticationResponse register(RegisterRequest registerRequest) {
    if (userRepository.existsByUsernameOrEmail(registerRequest.getUsername(), registerRequest.getEmail())) {
      throw new ConflictResourceException("Username or email already existed");
    }
    User user = User.builder()
        .username(registerRequest.getUsername())
        .email(registerRequest.getEmail())
        .password(passwordEncoder.encode(registerRequest.getPassword()))
        .role(Role.USER)
        .build();
    userRepository.save(user);
    return AuthenticationResponse.builder()
        .email(user.getEmail())
        .username(user.getUsername())
        .role(user.getRole())
        .build();
  }

  @Override
  public AuthenticationResponse refreshToken(String refreshToken) {
    if (refreshToken == null) {
      throw new BadCredentialsException("Refresh token is missing");
    }
    User user = userService.getCurrentUser();

    CustomUserDetails customUserDetails = new CustomUserDetails(user);

    if (!jwtService.isTokenValid(refreshToken, customUserDetails, "refresh")) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    String redisKey = "RT:" + user.getUsername();
    String redisToken = redisTemplate.opsForValue().get(redisKey);
    if (redisToken == null || !redisToken.equals(refreshToken)) {
      throw new BadCredentialsException("Refresh token expired or compromised");
    }

    String accessToken = jwtService.generateAccessToken(customUserDetails);
    String newRefreshToken = jwtService.generateRefreshToken(customUserDetails);

    redisTemplate.opsForValue().set(redisKey, newRefreshToken, Duration.ofDays(7));

    return AuthenticationResponse.builder()
        .email(user.getEmail())
        .username(user.getUsername())
        .role(user.getRole())
        .accessToken(accessToken)
        .refreshToken(newRefreshToken)
        .build();
  }

  @Override
  public void logout(String refreshToken) {
    if (refreshToken == null)
      return;
    try {
      String username = jwtService.extractUsername(refreshToken);
      if (username != null) {
        redisTemplate.delete("RT:" + username);
      }
    } catch (Exception e) {
      throw new BadCredentialsException("Invalid refresh token");
    }
  }

}

package com.market.prediction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.market.prediction.dto.request.LoginRequest;
import com.market.prediction.dto.request.RegisterRequest;
import com.market.prediction.dto.response.AuthenticationResponse;
import com.market.prediction.service.AuthenticationService;
import com.market.prediction.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {
  private final AuthenticationService authenticationService;
  private final CookieUtil cookieUtil;

  @PostMapping("/login")
  public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest loginRequest,
      HttpServletResponse httpServletResponse) {
    AuthenticationResponse authenticationResponse = authenticationService.login(loginRequest);

    cookieUtil.createCookie(httpServletResponse, CookieUtil.ACCESS_TOKEN_NAME, authenticationResponse.getAccessToken());
    cookieUtil.createCookie(httpServletResponse, CookieUtil.REFRESH_TOKEN_NAME,
        authenticationResponse.getRefreshToken());

    authenticationResponse.setAccessToken(null);
    authenticationResponse.setRefreshToken(null);
    return ResponseEntity.ok(authenticationResponse);
  }

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
    return ResponseEntity.ok(authenticationService.register(registerRequest));
  }

  @PostMapping("/refresh")
  public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_NAME);
    AuthenticationResponse authResponse = authenticationService.refreshToken(refreshToken);

    cookieUtil.createCookie(response, CookieUtil.ACCESS_TOKEN_NAME, authResponse.getAccessToken());
    cookieUtil.createCookie(response, CookieUtil.REFRESH_TOKEN_NAME, authResponse.getRefreshToken());

    return ResponseEntity.ok().build();
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_NAME);
    authenticationService.logout(refreshToken);
    cookieUtil.clearCookie(response, CookieUtil.ACCESS_TOKEN_NAME);
    cookieUtil.clearCookie(response, CookieUtil.REFRESH_TOKEN_NAME);
    return ResponseEntity.ok().build();
  }

}

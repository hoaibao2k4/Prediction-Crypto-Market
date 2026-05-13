package com.market.prediction.service;

import com.market.prediction.dto.request.LoginRequest;
import com.market.prediction.dto.request.RegisterRequest;
import com.market.prediction.dto.response.AuthenticationResponse;

public interface AuthenticationService {
    public AuthenticationResponse login(LoginRequest loginRequest);

    public AuthenticationResponse register(RegisterRequest registerRequest);

    public AuthenticationResponse refreshToken(String refreshToken);

    public void logout(String refreshToken);
}

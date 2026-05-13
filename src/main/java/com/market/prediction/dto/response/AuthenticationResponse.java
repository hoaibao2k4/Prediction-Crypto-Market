package com.market.prediction.dto.response;

import com.market.prediction.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AuthenticationResponse {
  String username;
  String email;
  Role role;
  String accessToken;
  String refreshToken;
}

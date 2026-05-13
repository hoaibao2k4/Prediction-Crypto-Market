package com.market.prediction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.market.prediction.dto.response.RoundResponse;
import com.market.prediction.service.RoundService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rounds")
@RequiredArgsConstructor
public class RoundController {
  private final RoundService roundService;

  @GetMapping("/current")
  public ResponseEntity<RoundResponse> getCurrentRound() {
    return ResponseEntity.ok(roundService.getCurrentRound());
  }

  @GetMapping("/time")
  public ResponseEntity<Long> getServerTime() {
    return ResponseEntity.ok(roundService.getServerTime());
  }
}


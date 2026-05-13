package com.market.prediction.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.market.prediction.dto.request.BetRequest;
import com.market.prediction.dto.response.BetResponse;
import com.market.prediction.dto.response.PageResponse;
import com.market.prediction.service.BetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bets")
@RequiredArgsConstructor
public class BetController {

    private final BetService betService;

    @PostMapping
    public ResponseEntity<Void> placeBet(
            @Valid @RequestBody BetRequest request) {
        betService.placeBet(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<BetResponse>> getHistory(
            Pageable pageable) {
        return ResponseEntity.ok(betService.getBetHistory(pageable));
    }
}

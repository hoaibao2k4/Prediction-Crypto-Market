package com.market.prediction.controller;

import com.market.prediction.dto.response.KlineResponse;
import com.market.prediction.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/klines")
    public ResponseEntity<List<KlineResponse>> getKlines(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "100") Integer limit) {
        
        List<KlineResponse> data = marketService.getKlines(symbol, interval, limit);
        return ResponseEntity.ok(data);
    }
}

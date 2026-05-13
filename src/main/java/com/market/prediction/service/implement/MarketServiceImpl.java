package com.market.prediction.service.implement;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.market.prediction.dto.response.KlineResponse;
import com.market.prediction.exception.BadRequestException;
import com.market.prediction.exception.BinanceProviderException;
import com.market.prediction.service.MarketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketServiceImpl implements MarketService {

    private static final Set<String> ALLOWED_INTERVALS = Set.of(
            "1s", "1m", "3m", "5m", "15m", "30m",
            "1h", "2h", "4h", "6h", "8h", "12h",
            "1d", "3d", "1w", "1M");
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 1000;

    private final RestTemplate restTemplate;

    @Value("${BINANCE_API_URL}")
    private String binanceApiUrl;

    @Override
    public List<KlineResponse> getKlines(String symbol, String interval, Integer limit) {
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedInterval = normalizeInterval(interval);
        int safeLimit = clampLimit(limit);
        String url = UriComponentsBuilder.fromUriString(binanceApiUrl)
                .path("/api/v3/klines")
                .queryParam("symbol", normalizedSymbol)
                .queryParam("interval", normalizedInterval)
                .queryParam("limit", safeLimit)
                .toUriString();
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response == null || !response.isArray()) {
                throw new BinanceProviderException("Invalid response from Binance API for symbol: " + normalizedSymbol);
            }

            return StreamSupport.stream(response.spliterator(), false)
                    .map(node -> KlineResponse.builder()
                            .openTime(node.get(0).asLong())
                            .open(new BigDecimal(node.get(1).asText()))
                            .high(new BigDecimal(node.get(2).asText()))
                            .low(new BigDecimal(node.get(3).asText()))
                            .close(new BigDecimal(node.get(4).asText()))
                            .volume(new BigDecimal(node.get(5).asText()))
                            .closeTime(node.get(6).asLong())
                            .build())
                    .collect(Collectors.toList());
        } catch (RestClientException e) {
            log.error("Failed to fetch klines from Binance for symbol {}: {}", normalizedSymbol, e.getMessage());
            throw new BinanceProviderException("Could not fetch kline data from Binance", e);
        }
    }

    private String normalizeSymbol(String symbol) {
        String normalizedSymbol = symbol == null ? "" : symbol.trim().replace("/", "").toUpperCase(Locale.ROOT);
        if (!normalizedSymbol.matches("[A-Z0-9]{5,20}")) {
            throw new BadRequestException("Invalid market symbol");
        }
        return normalizedSymbol;
    }

    private String normalizeInterval(String interval) {
        String normalizedInterval = interval == null ? "1m" : interval.trim();
        if (!ALLOWED_INTERVALS.contains(normalizedInterval)) {
            throw new BadRequestException("Invalid kline interval");
        }
        return normalizedInterval;
    }

    private int clampLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }
        return Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, limit));
    }
}

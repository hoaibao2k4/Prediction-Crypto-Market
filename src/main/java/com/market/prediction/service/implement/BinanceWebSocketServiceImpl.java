package com.market.prediction.service.implement;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.prediction.dto.response.PriceUpdateResponse;
import com.market.prediction.event.PriceUpdateEvent;
import com.market.prediction.exception.BinanceProviderException;
import com.market.prediction.service.BinanceWebSocketService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceWebSocketServiceImpl implements BinanceWebSocketService {

  private final ApplicationEventPublisher eventPublisher;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final RestTemplate restTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper mapper;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private static final String REDIS_KEY_LAST_UPDATE = "PRICE_LAST_UPDATE_TS";
  private volatile boolean isShuttingDown = false;
  private ConcurrentHashMap<String, BigDecimal> lastestPrices = new ConcurrentHashMap<>();
  private WebSocketClient client;

  @Value("${WS_BINACE_STREAM}")
  private String binanceUri;
  @Value("${SYMBOL_PRICE_API}")
  private String priceApi;

  @Override
  @PostConstruct
  public synchronized void initConnection() {
    if (client != null && !client.isClosed()) {
      client.close();
    }
    try {
      URI uri = new URI(binanceUri);
      client = new WebSocketClient(uri) {
        @Override
        public void onOpen(ServerHandshake serverHandshake) {
          log.info("Binance WebSocket Connected successfully");
          performCatchUp();
        }

        @Override
        public void onMessage(String msg) {
          handleBinanceMessage(msg);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
          if (isShuttingDown)
            return;
          log.warn("Binance WebSocket closed: {}. Reason: {}. Reconnecting in 5s...", code, reason);
          BinanceWebSocketServiceImpl.this.reconnect();
        }

        @Override
        public void onError(Exception ex) {
          log.error("Critical WebSocket error for Binance: {}", ex.getMessage());
          if (!isShuttingDown) {
            BinanceWebSocketServiceImpl.this.reconnect();
          }
        }
      };

      client.connect();
    } catch (Exception e) {
      log.error("Failed to initialize Binance connection: {}", e.getMessage());
      reconnect();
      throw new BinanceProviderException("Could not initialize Binance WebSocket connection", e);
    }
  }

  private void reconnect() {
    if (isShuttingDown)
      return;
    scheduler.schedule(() -> {
      log.info("Attempting to reconnect to Binance WebSocket...");
      initConnection();
    }, 5, TimeUnit.SECONDS);
  }

  @Scheduled(fixedRate = 60000)
  public void checkConnectionHealth() {
    if (isShuttingDown)
      return;
    Map<Object, Object> lastUpdateMap = redisTemplate.opsForHash().entries(REDIS_KEY_LAST_UPDATE);
    if (lastUpdateMap.isEmpty())
      return;

    long now = System.currentTimeMillis();
    boolean isStale = lastUpdateMap.values().stream()
        .map(obj -> Long.parseLong(obj.toString()))
        .anyMatch(ts -> (now - ts) > 60000); // 1 minute stale

    if (isStale) {
      log.warn("Connection health check failed (stale price data in Redis). Restarting WebSocket...");
      initConnection();
    }
  }

  @Override
  public void handleBinanceMessage(String msg) {
    if (isShuttingDown)
      return;
    try {
      JsonNode node = mapper.readTree(msg);
      if (node.has("data")) {
        JsonNode data = node.get("data");
        if (data.has("c") && data.has("s")) { // close price

          BigDecimal price = new BigDecimal(data.get("c").asText());
          String symbol = data.get("s").asText();
          Long eventTime = data.has("E") ? data.get("E").asLong() : System.currentTimeMillis();
          lastestPrices.put(symbol, price);

          // Persist last update time in Redis
          redisTemplate.opsForHash().put(REDIS_KEY_LAST_UPDATE, symbol, String.valueOf(System.currentTimeMillis()));

          log.debug("Price: {}", price);
          eventPublisher.publishEvent(new PriceUpdateEvent(this, symbol, price));

          PriceUpdateResponse priceResponse = PriceUpdateResponse.builder()
              .symbol(symbol)
              .price(price)
              .eventTime(eventTime)
              .build();
          simpMessagingTemplate.convertAndSend("/topic/price", priceResponse);
        }
      }

    } catch (Exception e) {
      log.error("Failed to process Binance message: {}. Error: ", msg, e);
      throw new BinanceProviderException("Could not process Binance message", e);
    }
  }

  @Override
  public BigDecimal getCurrentPrice(String pair) {
    BigDecimal price = lastestPrices.get(pair);
    if (price == null) {
      try {
        String api = priceApi + pair;
        JsonNode nodeRes = restTemplate.getForObject(api, JsonNode.class);
        if (nodeRes != null && nodeRes.has("price")) {
          price = new BigDecimal(nodeRes.get("price").asText());
          lastestPrices.put(pair, price);
        } else {
          throw new BinanceProviderException("Binance API returned invalid data for symbol: " + pair);
        }
      } catch (Exception e) {
        log.error("Failed to fetch price from Binance API for {}: {}", pair, e.getMessage());
        throw new BinanceProviderException("Could not fetch current price from Binance for " + pair, e);
      }
    }
    return price;
  }

  private void performCatchUp() {
    if (isShuttingDown)
      return;
    log.info("Starting price catch-up process...");
    Map<Object, Object> lastUpdateMap = redisTemplate.opsForHash().entries(REDIS_KEY_LAST_UPDATE);

    lastUpdateMap.forEach((symbolKey, lastTsObj) -> {
      String symbol = symbolKey.toString();
      long lastTs = Long.parseLong(lastTsObj.toString());
      long now = System.currentTimeMillis();

      if (now - lastTs > 2000) { // If gap > 2s
        try {
          String klineApi = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=1s&startTime="
              + lastTs + "&endTime=" + now;
          JsonNode klines = restTemplate.getForObject(klineApi, JsonNode.class);
          if (klines != null && klines.isArray()) {
            log.info("Catching up {} missing seconds for {}", klines.size(), symbol);
            for (JsonNode kline : klines) {
              BigDecimal high = new BigDecimal(kline.get(2).asText());
              BigDecimal low = new BigDecimal(kline.get(3).asText());
              eventPublisher.publishEvent(new PriceUpdateEvent(this, symbol, high));
              eventPublisher.publishEvent(new PriceUpdateEvent(this, symbol, low));
            }
          }
        } catch (Exception e) {
          log.error("Catch-up failed for symbol {}: {}", symbol, e.getMessage());
          throw new BinanceProviderException("Catch-up failed for " + symbol, e);
        }
      }
    });
  }

  @PreDestroy
  public void closeConnection() {
    this.isShuttingDown = true;
    if (client != null && !client.isClosed()) {
      log.info("Application is shutting down. Closing Binance WebSocket...");
      client.close();
    }
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

}

package com.market.prediction.service.implement;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.market.prediction.dto.response.RoundRedisResponse;
import com.market.prediction.enums.RoundResult;
import com.market.prediction.event.PriceUpdateEvent;
import com.market.prediction.service.RoundPriceMatcherService;
import com.market.prediction.service.RoundService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoundPriceMatcherServiceImpl implements RoundPriceMatcherService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final RoundService roundService;

  @EventListener
  public void handlePriceUpdateEvent(PriceUpdateEvent event) {
    processPriceUpdate(event.getPrice(), event.getSymbol());
  }

  @Override
  public void processPriceUpdate(BigDecimal currentPrice, String symbol) {
    Map<Object, Object> roundsForSymbol = redisTemplate.opsForHash().entries(getRedisKey(symbol));

    for (Object obj : roundsForSymbol.values()) {
      RoundRedisResponse currentRound = (RoundRedisResponse) obj;
      if (currentRound == null) {
        continue;
      }

      if (currentPrice.compareTo(currentRound.getTargetUp()) >= 0) {
        roundService.settleRound(RoundResult.UP, currentRound, symbol);
      } else if (currentPrice.compareTo(currentRound.getTargetDown()) <= 0) {
        roundService.settleRound(RoundResult.DOWN, currentRound, symbol);
      }
    }
  }

  private String getRedisKey(String symbol) {
    return "ACTIVE_ROUNDS:" + symbol;
  }
}

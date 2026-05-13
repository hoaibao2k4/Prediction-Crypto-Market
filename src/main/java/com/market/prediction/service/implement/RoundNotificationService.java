package com.market.prediction.service.implement;

import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.market.prediction.dto.response.BetNotificationResponse;
import com.market.prediction.dto.response.RoundRedisResponse;
import com.market.prediction.entity.Bet;
import com.market.prediction.entity.Round;
import com.market.prediction.event.RoundLockedEvent;
import com.market.prediction.event.RoundSettledEvent;
import com.market.prediction.event.RoundStartedEvent;
import com.market.prediction.event.RoundsCancelledEvent;
import com.market.prediction.mapper.RoundMapper;
import com.market.prediction.repository.BetRepository;
import com.market.prediction.service.RoundLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoundNotificationService {

  private final SimpMessagingTemplate simpMessagingTemplate;
  private final RoundMapper roundMapper;
  private final RedisTemplate<String, Object> redisTemplate;
  private final BetRepository betRepository;
  private final RoundLifecycleService roundLifecycleService;

  private static final String TOPIC_ROUND_STATUS = "/topic/round/status";
  private static final String TOPIC_ROUND = "/topic/round";
  private static final String QUEUE_NOTIFICATIONS = "/queue/notifications";

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRoundSettledEvent(RoundSettledEvent event) {
    redisTemplate.opsForHash().delete(getRedisKey(event.symbol()), event.round().getId().toString());
    simpMessagingTemplate.convertAndSend(TOPIC_ROUND_STATUS, roundMapper.toResponse(event.round()));

    List<Bet> bets = betRepository.findAllByRoundId(event.round().getId());
    bets.forEach(bet -> {
      String username = bet.getUser().getUsername();
      BetNotificationResponse notification = BetNotificationResponse.builder()
          .roundId(event.round().getId())
          .status(bet.getStatus())
          .message("Round " + event.round().getId() + " finished. You " + bet.getStatus() + "!")
          .build();
      simpMessagingTemplate.convertAndSendToUser(username, QUEUE_NOTIFICATIONS, notification);
    });

    log.info("Round {} settled and UI notified.", event.round().getId());
    roundLifecycleService.startNewRound();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRoundLockedEvent(RoundLockedEvent event) {
    simpMessagingTemplate.convertAndSend(TOPIC_ROUND_STATUS, roundMapper.toResponse(event.round()));
    log.info("Round {} transitioned to LOCKED.", event.round().getId());
    roundLifecycleService.startNewRound();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRoundsCancelledEvent(RoundsCancelledEvent event) {
    event.rounds().forEach(round -> {
      redisTemplate.opsForHash().delete(getRedisKey(round.getPair().getSymbol()), round.getId().toString());
      simpMessagingTemplate.convertAndSend(TOPIC_ROUND_STATUS, roundMapper.toResponse(round));
    });
    log.info("Cancelled {} expired rounds and notified UI.", event.rounds().size());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRoundStartedEvent(RoundStartedEvent event) {
    Round round = event.round();
    RoundRedisResponse redisResponse = RoundRedisResponse.builder()
        .id(round.getId())
        .targetDown(round.getTargetDown())
        .targetUp(round.getTargetUp())
        .symbol(round.getPair().getSymbol())
        .build();

    redisTemplate.opsForHash().put(getRedisKey(round.getPair().getSymbol()), round.getId().toString(), redisResponse);
    simpMessagingTemplate.convertAndSend(TOPIC_ROUND, roundMapper.toResponse(round));
    log.info("New round started for symbol: {}", round.getPair().getSymbol());
  }

  private String getRedisKey(String symbol) {
    return "ACTIVE_ROUNDS:" + symbol;
  }
}

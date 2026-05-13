package com.market.prediction.service.implement;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.market.prediction.dto.response.RoundRedisResponse;
import com.market.prediction.entity.Bet;
import com.market.prediction.entity.Pair;
import com.market.prediction.entity.Round;
import com.market.prediction.enums.BetStatus;
import com.market.prediction.enums.RoundResult;
import com.market.prediction.enums.RoundStatus;
import com.market.prediction.event.RoundLockedEvent;
import com.market.prediction.event.RoundStartedEvent;
import com.market.prediction.event.RoundsCancelledEvent;
import com.market.prediction.repository.BetRepository;
import com.market.prediction.repository.PairRepository;
import com.market.prediction.repository.RoundRepository;
import com.market.prediction.service.BinanceWebSocketService;
import com.market.prediction.service.RoundLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoundLifecycleServiceImpl implements RoundLifecycleService {

  private final RoundRepository roundRepository;
  private final PairRepository pairRepository;
  private final BetRepository betRepository;
  private final BinanceWebSocketService binanceWebSocketService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ApplicationEventPublisher eventPublisher;
  private final PlatformTransactionManager transactionManager;

  private static final Random RANDOM = new Random();
  private final Object startRoundLock = new Object();

  @Override
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void init() {
    log.info("Loading active rounds from DB to Redis...");

    List<Round> activeRounds = roundRepository.findByStatusIn(List.of(RoundStatus.LOCKED, RoundStatus.VOTING));

    activeRounds.forEach(round -> {
      if (round.getStatus() == RoundStatus.VOTING) {
        long secondsSinceCreation = Duration.between(round.getCreatedAt(), LocalDateTime.now()).getSeconds();
        if (secondsSinceCreation > 180) {
          log.info("Round {} was VOTING but expired during downtime ({}s). Moving to LOCKED.",
              round.getId(), secondsSinceCreation);
          round.setStatus(RoundStatus.LOCKED);
          roundRepository.save(round);
        }
      }

      Long roundId = round.getId();
      String symbol = round.getPair().getSymbol();
      if (roundId == null || symbol == null) {
        return;
      }

      RoundRedisResponse redisResponse = RoundRedisResponse.builder()
          .id(roundId)
          .targetDown(round.getTargetDown())
          .targetUp(round.getTargetUp())
          .symbol(symbol)
          .build();

      redisTemplate.opsForHash().put(getRedisKey(symbol), roundId.toString(), redisResponse);
    });

    log.info("Recovered {} active rounds to Redis.", activeRounds.size());

    if (!roundRepository.existsByStatus(RoundStatus.VOTING)) {
      startNewRound();
    }
  }

  @Override
  @Scheduled(fixedRate = 1000)
  @Transactional
  public void checkCountdown() {
    Optional<Round> roundOpt = roundRepository.findFirstByStatusOrderByCreatedAtDesc(RoundStatus.VOTING);
    if (roundOpt.isEmpty()) {
      startNewRound();
      return;
    }

    Round currentRound = roundOpt.get();
    long seconds = Duration.between(currentRound.getCreatedAt(), LocalDateTime.now()).getSeconds();

    if (seconds > 180) {
      currentRound.setStatus(RoundStatus.LOCKED);
      Round savedRound = roundRepository.save(currentRound);
      eventPublisher.publishEvent(new RoundLockedEvent(savedRound));
    }
  }

  @Override
  @Scheduled(cron = "0 0 22 * * *", zone = "UTC")
  @Transactional
  public void cancelExpiredRounds() {
    List<Round> lockedRounds = roundRepository.findByStatus(RoundStatus.LOCKED);

    if (lockedRounds.isEmpty()) {
      return;
    }

    log.info("Found {} locked rounds at 22:00 UTC. Cancelling...", lockedRounds.size());

    List<Long> ids = lockedRounds.stream().map(Round::getId).toList();
    List<Bet> bets = betRepository.findAllByRoundIdIn(ids);

    lockedRounds.forEach(round -> {
      round.setStatus(RoundStatus.CANCELLED);
      round.setResult(RoundResult.NA);
      round.setSettledAt(LocalDateTime.now());
    });

    bets.forEach(bet -> bet.setStatus(BetStatus.TIE));

    betRepository.saveAll(bets);
    roundRepository.saveAll(lockedRounds);

    eventPublisher.publishEvent(new RoundsCancelledEvent(lockedRounds));
  }

  @Override
  public void startNewRound() {
    synchronized (startRoundLock) {
      TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
      transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
      transactionTemplate.executeWithoutResult(status -> createRoundIfNeeded());
    }
  }

  private void createRoundIfNeeded() {
    if (roundRepository.existsByStatus(RoundStatus.VOTING)) {
      return;
    }

    List<Pair> pairs = pairRepository.findAll();
    if (pairs.isEmpty()) {
      log.warn("No pairs found in db.");
      return;
    }

    Pair pair = pairs.get(RANDOM.nextInt(pairs.size()));
    BigDecimal openPrice = binanceWebSocketService.getCurrentPrice(pair.getSymbol());
    if (openPrice == null) {
      log.error("Failed to start new round: Could not fetch price for symbol {}", pair.getSymbol());
      return;
    }

    Round round = Round.builder()
        .pair(pair)
        .openPrice(openPrice)
        .targetUp(openPrice.multiply(new BigDecimal("1.0003")))
        .targetDown(openPrice.multiply(new BigDecimal("0.99997")))
        .status(RoundStatus.VOTING)
        .build();

    roundRepository.save(round);
    eventPublisher.publishEvent(new RoundStartedEvent(round));
  }

  private String getRedisKey(String symbol) {
    return "ACTIVE_ROUNDS:" + symbol;
  }
}

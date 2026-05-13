package com.market.prediction.service.implement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.market.prediction.dto.response.RoundRedisResponse;
import com.market.prediction.dto.response.RoundResponse;
import com.market.prediction.entity.Bet;
import com.market.prediction.entity.Round;
import com.market.prediction.enums.BetStatus;
import com.market.prediction.enums.RoundResult;
import com.market.prediction.enums.RoundStatus;
import com.market.prediction.event.RoundSettledEvent;
import com.market.prediction.exception.ResourceNotFoundException;
import com.market.prediction.mapper.RoundMapper;
import com.market.prediction.repository.BetRepository;
import com.market.prediction.repository.RoundRepository;
import com.market.prediction.service.RoundService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundServiceImpl implements RoundService {

	private final RoundRepository roundRepository;
	private final RoundMapper roundMapper;
	private final ApplicationEventPublisher eventPublisher;
	private final BetRepository betRepository;

	@Override
	public RoundResponse getCurrentRound() {
		return roundRepository.findFirstByOrderByCreatedAtDesc().map(roundMapper::toResponse).orElse(null);
	}

	@Override
	public long getServerTime() {
		return System.currentTimeMillis();
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	@Async
	public void settleRound(RoundResult roundResult, RoundRedisResponse currentRound, String symbol) {
		log.info("Asynchronously settling round {} with result {} on thread {}",
				currentRound.getId(), roundResult, Thread.currentThread().getName());

		LocalDateTime settledAt = LocalDateTime.now();
		int updated = roundRepository.settleRoundAtomically(currentRound.getId(), roundResult, RoundStatus.SETTLED, settledAt);

		if (updated == 0) {
			log.warn("Round {} already settled or invalid state. Skipping.", currentRound.getId());
			return;
		}

		Round round = roundRepository.findById(currentRound.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Round not found"));

		List<Bet> bets = betRepository.findAllByRoundId(currentRound.getId());
		bets.forEach(bet -> {
			BetStatus status = bet.getPrediction().name().equals(roundResult.name()) ? BetStatus.WIN : BetStatus.LOSE;
			bet.setStatus(status);
		});
		betRepository.saveAll(bets);

		eventPublisher.publishEvent(new RoundSettledEvent(round, roundResult, symbol));
	}

}

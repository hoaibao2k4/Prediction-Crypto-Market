package com.market.prediction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.market.prediction.dto.response.RoundRedisResponse;
import com.market.prediction.dto.response.RoundResponse;
import com.market.prediction.entity.Bet;
import com.market.prediction.entity.Round;
import com.market.prediction.enums.BetPrediction;
import com.market.prediction.enums.BetStatus;
import com.market.prediction.enums.RoundResult;
import com.market.prediction.enums.RoundStatus;
import com.market.prediction.event.RoundSettledEvent;
import com.market.prediction.mapper.RoundMapper;
import com.market.prediction.repository.BetRepository;
import com.market.prediction.repository.RoundRepository;
import com.market.prediction.service.implement.RoundServiceImpl;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundMapper roundMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private RoundServiceImpl roundService;

    private Round mockRound;
    private RoundRedisResponse redisResponse;

    @BeforeEach
    void setUp() {
        mockRound = Round.builder().id(1L).status(RoundStatus.LOCKED).build();
        redisResponse = RoundRedisResponse.builder().id(1L).symbol("BTCUSDT").build();
    }

    @Test
    void getCurrentRound_Success() {
        // Arrange
        when(roundRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(mockRound));
        when(roundMapper.toResponse(mockRound)).thenReturn(new RoundResponse());

        // Act
        RoundResponse response = roundService.getCurrentRound();

        // Assert
        assertNotNull(response);
        verify(roundRepository).findFirstByOrderByCreatedAtDesc();
    }

    @Test
    void settleRound_Success() {
        // Arrange
        Bet mockBet = Bet.builder().id(10L).prediction(BetPrediction.UP).status(BetStatus.PENDING).build();
        
        when(roundRepository.settleRoundAtomically(eq(1L), eq(RoundResult.UP), eq(RoundStatus.SETTLED), any()))
                .thenReturn(1);
        when(roundRepository.findById(1L)).thenReturn(Optional.of(mockRound));
        when(betRepository.findAllByRoundId(1L)).thenReturn(List.of(mockBet));

        // Act
        roundService.settleRound(RoundResult.UP, redisResponse, "BTCUSDT");

        // Assert
        assertEquals(BetStatus.WIN, mockBet.getStatus());
        verify(betRepository).saveAll(any());
        verify(eventPublisher).publishEvent(any(RoundSettledEvent.class));
    }

    @Test
    void settleRound_AlreadySettled_ShouldSkip() {
        // Arrange
        when(roundRepository.settleRoundAtomically(eq(1L), eq(RoundResult.UP), eq(RoundStatus.SETTLED), any()))
                .thenReturn(0);

        // Act
        roundService.settleRound(RoundResult.UP, redisResponse, "BTCUSDT");

        // Assert
        verify(roundRepository, never()).findById(any());
        verify(betRepository, never()).findAllByRoundId(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}

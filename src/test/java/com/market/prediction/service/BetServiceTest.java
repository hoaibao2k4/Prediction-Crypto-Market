package com.market.prediction.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.market.prediction.dto.request.BetRequest;
import com.market.prediction.entity.Round;
import com.market.prediction.entity.User;
import com.market.prediction.enums.BetPrediction;
import com.market.prediction.enums.RoundStatus;
import com.market.prediction.exception.BadRequestException;
import com.market.prediction.exception.ConflictResourceException;
import com.market.prediction.repository.BetRepository;
import com.market.prediction.repository.RoundRepository;
import com.market.prediction.service.implement.BetServiceImpl;

@ExtendWith(MockitoExtension.class)
class BetServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BetServiceImpl betService;

    private User mockUser;
    private Round mockRound;
    private BetRequest betRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).username("testuser").build();
        mockRound = Round.builder().id(100L).status(RoundStatus.VOTING).build();
        betRequest = new BetRequest();
        betRequest.setRoundId(100L);
        betRequest.setPrediction(BetPrediction.UP);
    }

    @Test
    void placeBet_Success() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(roundRepository.findById(100L)).thenReturn(Optional.of(mockRound));
        when(betRepository.existsByUserIdAndRoundId(1L, 100L)).thenReturn(false);

        // Act
        betService.placeBet(betRequest);

        // Assert
        verify(betRepository).save(any());
    }

    @Test
    void placeBet_RoundNotVoting_ShouldThrowException() {
        // Arrange
        mockRound.setStatus(RoundStatus.LOCKED);
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(roundRepository.findById(100L)).thenReturn(Optional.of(mockRound));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> betService.placeBet(betRequest));
        verify(betRepository, never()).save(any());
    }

    @Test
    void placeBet_AlreadyBet_ShouldThrowException() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(roundRepository.findById(100L)).thenReturn(Optional.of(mockRound));
        when(betRepository.existsByUserIdAndRoundId(1L, 100L)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictResourceException.class, () -> betService.placeBet(betRequest));
        verify(betRepository, never()).save(any());
    }
}

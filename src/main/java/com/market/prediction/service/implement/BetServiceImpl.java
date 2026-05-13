package com.market.prediction.service.implement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.market.prediction.dto.request.BetRequest;
import com.market.prediction.dto.response.BetResponse;
import com.market.prediction.dto.response.PageResponse;
import com.market.prediction.entity.Bet;
import com.market.prediction.entity.Round;
import com.market.prediction.entity.User;
import com.market.prediction.enums.BetStatus;
import com.market.prediction.enums.RoundStatus;
import com.market.prediction.exception.BadRequestException;
import com.market.prediction.exception.ConflictResourceException;
import com.market.prediction.mapper.BetMapper;
import com.market.prediction.repository.BetRepository;
import com.market.prediction.repository.RoundRepository;
import com.market.prediction.service.BetService;
import com.market.prediction.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BetServiceImpl implements BetService {

    private final BetRepository betRepository;
    private final RoundRepository roundRepository;
    private final BetMapper betMapper;
    private final UserService userService;

    @Override
    @Transactional
    public void placeBet(BetRequest betRequest) {
        User user = userService.getCurrentUser();
        Round round = roundRepository.findById(betRequest.getRoundId())
                .orElseThrow(() -> new BadRequestException("Invalid input"));
        if (round.getStatus() != RoundStatus.VOTING) {
            log.error("Invalid round");
            throw new BadRequestException("This round had closed");
        }
        if (betRepository.existsByUserIdAndRoundId(user.getId(), round.getId())) {
            throw new ConflictResourceException("User has already bet");
        }

        Bet bet = Bet.builder()
                .round(round)
                .user(user)
                .prediction(betRequest.getPrediction())
                .status(BetStatus.PENDING)
                .build();
        betRepository.save(bet);
    }

    @Override
    public PageResponse<BetResponse> getBetHistory(Pageable pageable) {
        User user = userService.getCurrentUser();
        Page<Bet> betPage = betRepository.findByUser(user, pageable);
        
        return PageResponse.<BetResponse>builder()
                .content(betPage.getContent().stream().map(betMapper::toResponse).toList())
                .totalElements(betPage.getTotalElements())
                .totalPages(betPage.getTotalPages())
                .page(betPage.getNumber())
                .size(betPage.getSize())
                .build();
    }
}

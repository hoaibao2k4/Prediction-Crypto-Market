package com.market.prediction.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.market.prediction.entity.Bet;
import com.market.prediction.entity.User;

public interface BetRepository extends JpaRepository<Bet, Long> {
    @EntityGraph(attributePaths = {"round", "round.pair"})
    Page<Bet> findByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"round", "round.pair", "user"})
    List<Bet> findAllByRoundId(Long roundId);

    boolean existsByUserIdAndRoundId(Long userId, Long roundId);

    @EntityGraph(attributePaths = {"round", "round.pair"})
    List<Bet> findAllByRoundIdIn(List<Long> roundIds);
}

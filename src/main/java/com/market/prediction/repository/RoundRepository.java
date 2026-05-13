package com.market.prediction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.market.prediction.entity.Round;
import com.market.prediction.enums.RoundResult;
import com.market.prediction.enums.RoundStatus;

public interface RoundRepository extends JpaRepository<Round, Long> {
    @EntityGraph(attributePaths = { "pair" })
    List<Round> findByStatus(RoundStatus status);

    Optional<Round> findFirstByStatusOrderByCreatedAtDesc(RoundStatus status);

    boolean existsByStatus(RoundStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Round r SET r.status = :newStatus, r.result = :result, r.settledAt = :settledAt " +
           "WHERE r.id = :id AND r.status IN (com.market.prediction.enums.RoundStatus.VOTING, com.market.prediction.enums.RoundStatus.LOCKED)")
    int settleRoundAtomically(@Param("id") Long id, 
                              @Param("result") RoundResult result, 
                              @Param("newStatus") RoundStatus newStatus, 
                              @Param("settledAt") LocalDateTime settledAt);

    @EntityGraph(attributePaths = { "pair" })
    List<Round> findByStatusIn(List<RoundStatus> statuses);

    Optional<Round> findFirstByOrderByCreatedAtDesc();

}

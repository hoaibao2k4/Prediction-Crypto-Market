package com.market.prediction.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.market.prediction.entity.UserBetLimit;

@Repository
public interface UserBetLimitRepository extends JpaRepository<UserBetLimit, Long> {
  @Modifying(clearAutomatically = true)
  @Query("""
      Update UserBetLimit q
      Set q.limitCount =q.limitCount +1
      Where q.user.id = :userId
      and q.betDate = :betDate
      and q.limitCount < :limit
      """)
  int incrementIfNotLimit(@Param("userId") Long userId, @Param("betDate") LocalDate betDate, @Param("limit") int limit);
@Modifying(clearAutomatically = true)
@Query(value = """
    INSERT IGNORE INTO user_bet_limit (user_id, bet_date, limit_count)
    VALUES (:userId, :betDate, 0)
    """, nativeQuery = true)
    void ensureTodayQuotaRow(@Param("userId") Long userId, @Param("betDate") LocalDate betDate);
}

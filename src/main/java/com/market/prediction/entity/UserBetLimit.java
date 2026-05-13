package com.market.prediction.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_bet_limit", indexes = {
    @Index(name = "idx_user_bet_limit_user_id_bet_date", columnList = "user_id, bet_date")
})
public class UserBetLimit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "limit_id")
  private Long limitId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "bet_date")
  private LocalDate betDate;

  @Column(name = "limit_count")
  private int limitCount;
}

package com.market.prediction.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.market.prediction.enums.RoundResult;
import com.market.prediction.enums.RoundStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "rounds", indexes = {
    @Index(name = "idx_round_status_created_at", columnList = "status, created_at DESC")
})
public class Round extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pair_id", nullable = false)
    private Pair pair;

    @Column(name = "open_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal openPrice;

    @Column(name = "target_up", nullable = false, precision = 18, scale = 8)
    private BigDecimal targetUp;

    @Column(name = "target_down", nullable = false, precision = 18, scale = 8)
    private BigDecimal targetDown;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RoundStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 20)
    private RoundResult result;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Version
    private Integer version;
}

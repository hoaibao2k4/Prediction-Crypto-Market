package com.market.prediction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.market.prediction.enums.RoundStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RoundResponse extends RoundRedisResponse {
    private RoundStatus status;
    private BigDecimal openPrice;
    private LocalDateTime createdAt;
    private long serverTime;
}

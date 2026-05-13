package com.market.prediction.dto.response;

import java.math.BigDecimal;

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
public class RoundRedisResponse {
    private Long id;
    private BigDecimal targetUp;
    private BigDecimal targetDown;
    private String symbol;
}

package com.market.prediction.dto.response;

import java.time.LocalDateTime;
import com.market.prediction.enums.BetPrediction;
import com.market.prediction.enums.BetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetResponse {
    private Long id;
    private Long roundId;
    private String pairSymbol;
    private BetPrediction prediction;
    private BetStatus status;
    private LocalDateTime createdAt;
}

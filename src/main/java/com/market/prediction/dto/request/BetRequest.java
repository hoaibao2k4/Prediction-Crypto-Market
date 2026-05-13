package com.market.prediction.dto.request;

import com.market.prediction.enums.BetPrediction;
import jakarta.validation.constraints.NotNull;
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
public class BetRequest {
    
    @NotNull(message = "Round ID is required")
    private Long roundId;
    
    @NotNull(message = "Prediction (UP/DOWN) is required")
    private BetPrediction prediction;
}

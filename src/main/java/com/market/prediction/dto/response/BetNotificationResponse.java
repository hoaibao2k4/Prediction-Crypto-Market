package com.market.prediction.dto.response;

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
public class BetNotificationResponse {
    private Long roundId;
    private BetStatus status;
    private String message;
}

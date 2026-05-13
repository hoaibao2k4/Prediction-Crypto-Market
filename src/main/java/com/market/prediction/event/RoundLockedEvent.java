package com.market.prediction.event;

import com.market.prediction.entity.Round;

public record RoundLockedEvent(Round round) {
}

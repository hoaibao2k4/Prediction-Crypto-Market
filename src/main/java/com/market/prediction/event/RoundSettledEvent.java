package com.market.prediction.event;

import com.market.prediction.entity.Round;
import com.market.prediction.enums.RoundResult;

public record RoundSettledEvent(Round round, RoundResult result, String symbol) {

}

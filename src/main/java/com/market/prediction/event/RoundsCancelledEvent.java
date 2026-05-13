package com.market.prediction.event;

import java.util.List;
import com.market.prediction.entity.Round;

public record RoundsCancelledEvent(List<Round> rounds) {
}

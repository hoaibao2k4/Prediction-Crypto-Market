package com.market.prediction.service;

import com.market.prediction.dto.response.RoundRedisResponse;
import com.market.prediction.dto.response.RoundResponse;
import com.market.prediction.enums.RoundResult;

public interface RoundService {

  RoundResponse getCurrentRound();

  long getServerTime();

  void settleRound(RoundResult roundResult, RoundRedisResponse currentRound, String symbol);
}

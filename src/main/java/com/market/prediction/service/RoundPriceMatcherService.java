package com.market.prediction.service;

import java.math.BigDecimal;

public interface RoundPriceMatcherService {

  void processPriceUpdate(BigDecimal currentPrice, String symbol);
}

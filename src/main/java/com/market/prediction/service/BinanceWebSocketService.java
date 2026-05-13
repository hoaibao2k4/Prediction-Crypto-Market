package com.market.prediction.service;

import java.math.BigDecimal;

public interface BinanceWebSocketService {
  public void initConnection();

  public void handleBinanceMessage(String msg);

  public BigDecimal getCurrentPrice(String pair);
}
    

package com.market.prediction.service;

public interface RoundLifecycleService {

  void init();

  void checkCountdown();

  void cancelExpiredRounds();

  void startNewRound();
}

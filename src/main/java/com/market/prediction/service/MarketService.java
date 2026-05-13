package com.market.prediction.service;

import com.market.prediction.dto.response.KlineResponse;
import java.util.List;

public interface MarketService {
    List<KlineResponse> getKlines(String symbol, String interval, Integer limit);
}

package com.market.prediction.service;

import org.springframework.data.domain.Pageable;

import com.market.prediction.dto.request.BetRequest;
import com.market.prediction.dto.response.BetResponse;
import com.market.prediction.dto.response.PageResponse;

public interface BetService {
    void placeBet(BetRequest request);

    PageResponse<BetResponse> getBetHistory(Pageable pageable);
}

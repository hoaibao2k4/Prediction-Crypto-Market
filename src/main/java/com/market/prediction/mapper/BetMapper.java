package com.market.prediction.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.market.prediction.entity.Bet;
import com.market.prediction.dto.response.BetResponse;

@Mapper(componentModel = "spring")
public interface BetMapper {
    
    @Mapping(target = "roundId", source = "round.id")
    @Mapping(target = "pairSymbol", source = "round.pair.symbol")
    BetResponse toResponse(Bet bet);
}

package com.market.prediction.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.market.prediction.dto.response.RoundResponse;
import com.market.prediction.entity.Round;

@Mapper(componentModel = "spring")
public interface RoundMapper {

    @Mapping(target = "symbol", source = "pair.symbol")
    @Mapping(target = "serverTime", expression = "java(System.currentTimeMillis())")
    RoundResponse toResponse(Round round);
}

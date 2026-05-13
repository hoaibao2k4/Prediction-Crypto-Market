package com.market.prediction.event;

import java.math.BigDecimal;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class PriceUpdateEvent extends ApplicationEvent {
    private final String symbol;
    private final BigDecimal price;

    public PriceUpdateEvent(Object source, String symbol, BigDecimal price) {
        super(source);
        this.symbol = symbol;
        this.price = price;
    }
}

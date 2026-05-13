package com.market.prediction.exception;

public class BinanceProviderException extends RuntimeException {
    public BinanceProviderException(String message) {
        super(message);
    }

    public BinanceProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}

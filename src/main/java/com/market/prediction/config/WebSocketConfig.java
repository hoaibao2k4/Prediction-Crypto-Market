package com.market.prediction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
  private String allowedOrigins;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) {
    messageBrokerRegistry.enableSimpleBroker("/topic", "/queue");
    messageBrokerRegistry.setApplicationDestinationPrefixes("/app");
    messageBrokerRegistry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry stompEndpointRegistry) {
    String[] origins = allowedOrigins.split(",");
    stompEndpointRegistry.addEndpoint("/ws")
        .setAllowedOrigins(origins)
        .withSockJS();
  }
}

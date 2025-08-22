package com.testtask.config;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RequestIdFilter implements WebFilter {
    public static final String HEADER = "X-Request-ID";
    public static final String MDC_KEY = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = Optional.ofNullable(request.getHeaders().getFirst(HEADER))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        return chain.filter(exchange.mutate()
                        .request(builder -> builder.headers(http -> http.set(HEADER, requestId)))
                        .build())
                .doFirst(() -> MDC.put(MDC_KEY, requestId))
                .doFinally(sig -> MDC.remove(MDC_KEY));
    }
}

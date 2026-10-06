package io.reqover.example.webflux.auto;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AutoReactiveOrderService {
    public Mono<AutoReactiveOrderResponse> find(long id) {
        return Mono.just(id)
                .publishOn(Schedulers.boundedElastic())
                .map(this::validate)
                .publishOn(Schedulers.parallel())
                .map(this::toResponse);
    }

    private long validate(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        return id;
    }

    private AutoReactiveOrderResponse toResponse(long id) {
        return new AutoReactiveOrderResponse(id, "AUTO_REACTIVE_FOUND");
    }
}


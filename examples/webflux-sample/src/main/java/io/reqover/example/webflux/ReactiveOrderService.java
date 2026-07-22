package io.reqover.example.webflux;

import io.reqover.core.ReqoverProbe;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ReactiveOrderService {
    private final ReactiveOrderMapper mapper;
    private final ReactiveValidator validator;

    public ReactiveOrderService(ReactiveOrderMapper mapper, ReactiveValidator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public Mono<ReactiveOrderResponse> find(long id) {
        return Mono.just(id)
                .publishOn(Schedulers.boundedElastic())
                .map(value -> {
                    ReqoverProbe.hit(ProbeIds.REACTIVE_ORDER_SERVICE, 1);
                    validator.validate("order-" + value);
                    return value;
                })
                .publishOn(Schedulers.parallel())
                .map(mapper::toResponse);
    }
}


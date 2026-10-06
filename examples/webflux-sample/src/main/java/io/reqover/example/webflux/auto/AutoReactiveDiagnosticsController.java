package io.reqover.example.webflux.auto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/** Uses reactive delay; a Mono factory returning is not the end of a request. */
@RestController
public class AutoReactiveDiagnosticsController {
    @GetMapping("/auto/reactive/diagnostics/delay/{millis}")
    public Mono<Map<String, Long>> delay(@PathVariable("millis") long millis) {
        if (millis < 0 || millis > 2000) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "delay must be between 0 and 2000 ms"));
        }
        return Mono.delay(Duration.ofMillis(millis)).map(ignored -> Map.of("requestedDelayMillis", millis));
    }

    @GetMapping("/auto/reactive/diagnostics/failure")
    public Mono<ResponseEntity<Map<String, String>>> failure() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("reason", "intentional diagnostic demo failure")));
    }
}

package io.reqover.example.mvc.auto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * An endpoint whose only purpose is to execute a caller-chosen number of
 * instrumented method entries, so the agent's cost can be measured as a
 * function of how much instrumented code a request walks through.
 *
 * <p>The ordinary demo endpoint enters four instrumented methods per request,
 * which is too few to separate the agent's cost from measurement noise. Asking
 * this endpoint for several thousand entries makes the per-entry cost visible,
 * and comparing two depths gives the slope.
 */
@RestController
public class AutoDepthController {
    /**
     * Bounded well below the default thread stack so a request cannot take the
     * application down with a {@code StackOverflowError}.
     */
    static final int MAX_DEPTH = 5000;

    private final AutoDepthService depthService;

    public AutoDepthController(AutoDepthService depthService) {
        this.depthService = depthService;
    }

    @GetMapping("/auto/depth/{depth}")
    AutoDepthResponse walk(@PathVariable int depth) {
        if (depth < 0 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("depth must be between 0 and " + MAX_DEPTH);
        }
        return new AutoDepthResponse(depth, depthService.walk(depth));
    }
}

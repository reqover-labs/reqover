package io.reqover.example.mvc.auto;

import org.springframework.stereotype.Service;

/**
 * Executes a requested number of instrumented method entries.
 *
 * <p>Each recursion level is one probe hit on the same probe, which is the
 * common case in real code: a method is entered many times, and the bucket
 * records a set, so the repeat cost is the lookup and the set add rather than
 * growth. Measuring that path is the point — it is what a request pays per
 * instrumented method entry.
 */
@Service
public class AutoDepthService {
    long walk(int remaining) {
        if (remaining <= 0) {
            return 0L;
        }
        return remaining + walk(remaining - 1);
    }
}

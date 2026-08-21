package io.reqover.core;

import java.util.Map;
import java.util.Objects;

/**
 * Describes the logical unit of work that owns a coverage bucket.
 *
 * <p>An HTTP request is the unit Reqover attributes automatically, but the
 * record is deliberately generic: a scheduled job, a consumed message, or a
 * single test case is just as valid an owner. {@link UnitScope} opens a bucket
 * for those, and the report groups by {@link #name()} whatever the type is.
 */
public record UnitInfo(
        String unitId,
        String unitType,
        String name,
        Map<String, String> attributes
) {
    /** Unit type for an inbound HTTP request handled by an MVC or WebFlux adapter. */
    public static final String TYPE_HTTP_REQUEST = "http-request";
    /** Unit type for a scheduled or batch job run. */
    public static final String TYPE_SCHEDULED_JOB = "scheduled-job";
    /** Unit type for a consumed message or event. */
    public static final String TYPE_MESSAGE = "message";
    /** Unit type for a single test case. */
    public static final String TYPE_TEST = "test";
    /** Unit type for hits recorded outside any unit of work. */
    public static final String TYPE_GLOBAL = "global";

    public UnitInfo {
        unitId = requireText(unitId, "unitId");
        unitType = requireText(unitType, "unitType");
        name = requireText(name, "name");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }

    public static UnitInfo httpRequest(String requestId, String method, String endpointPattern) {
        return new UnitInfo(
                requestId,
                TYPE_HTTP_REQUEST,
                method + " " + endpointPattern,
                Map.of("method", method, "endpointPattern", endpointPattern)
        );
    }

    /**
     * A unit of work that is not an HTTP request. {@code name} is what the
     * report groups by, so it should identify the job, topic, or test rather
     * than the individual run.
     */
    public static UnitInfo of(String unitId, String unitType, String name) {
        return new UnitInfo(unitId, unitType, name, Map.of());
    }

    public static UnitInfo scheduledJob(String runId, String jobName) {
        return new UnitInfo(runId, TYPE_SCHEDULED_JOB, jobName, Map.of("jobName", jobName));
    }

    public static UnitInfo message(String messageId, String destination) {
        return new UnitInfo(messageId, TYPE_MESSAGE, destination, Map.of("destination", destination));
    }

    public static UnitInfo test(String runId, String testName) {
        return new UnitInfo(runId, TYPE_TEST, testName, Map.of("testName", testName));
    }

    public static UnitInfo global() {
        return new UnitInfo("global", TYPE_GLOBAL, "global", Map.of());
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}


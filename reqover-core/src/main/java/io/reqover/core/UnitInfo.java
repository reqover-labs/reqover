package io.reqover.core;

import java.util.Map;
import java.util.Objects;

/**
 * Describes the logical unit of work that owns a coverage bucket.
 */
public record UnitInfo(
        String unitId,
        String unitType,
        String name,
        Map<String, String> attributes
) {
    public UnitInfo {
        unitId = requireText(unitId, "unitId");
        unitType = requireText(unitType, "unitType");
        name = requireText(name, "name");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }

    public static UnitInfo httpRequest(String requestId, String method, String endpointPattern) {
        return new UnitInfo(
                requestId,
                "http-request",
                method + " " + endpointPattern,
                Map.of("method", method, "endpointPattern", endpointPattern)
        );
    }

    public static UnitInfo global() {
        return new UnitInfo("global", "global", "global", Map.of());
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}


package io.reqover.report;

import java.util.List;

/**
 * An observed endpoint that executed at least one of the changed methods.
 *
 * @param endpoint the endpoint as the report records it, such as {@code GET /orders/{id}}
 * @param changedCode the changed methods this endpoint was observed executing
 */
public record ImpactedEndpoint(String endpoint, List<CodeRef> changedCode) {
}

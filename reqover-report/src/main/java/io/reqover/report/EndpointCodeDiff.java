package io.reqover.report;

import java.util.List;

/**
 * How the code executed by one endpoint changed between two reports.
 *
 * @param endpoint the endpoint observed in both reports
 * @param addedCode methods the current report saw that the baseline did not
 * @param removedCode methods the baseline saw that the current report did not
 */
public record EndpointCodeDiff(String endpoint, List<CodeRef> addedCode, List<CodeRef> removedCode) {
}

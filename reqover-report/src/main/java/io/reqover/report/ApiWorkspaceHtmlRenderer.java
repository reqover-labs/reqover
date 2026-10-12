/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.report;

/** Read-only aggregation and explicit test-target selection, separate from code impact. */
final class ApiWorkspaceHtmlRenderer {
    private ApiWorkspaceHtmlRenderer() { }

    static String section() {
        return """
                <section class="section" id="api-workspace" hidden>
                  <div class="api-heading"><div><span class="api-eyebrow">RECORDED HTTP TRAFFIC</span><h2>API workspace</h2></div>
                    <div class="map-modes" role="group" aria-label="API workspace view">
                      <button type="button" data-api-view="groups" aria-pressed="true">Groups</button>
                      <button type="button" data-api-view="table" aria-pressed="false">API list</button>
                    </div>
                  </div>
                  <div id="api-totals" class="api-totals"></div>
                  <p class="api-scope">All retained HTTP observations, not total production traffic or TPS. Path groups are not service dependencies.</p>
                  <div class="api-toolbar">
                    <label class="api-search">Search API<input id="api-search" type="search" placeholder="/orders or GET" autocomplete="off"></label>
                    <label>HTTP status<select id="api-status"><option value="all">All statuses</option><option value="5xx">Has 5xx</option><option value="4xx">Has 4xx</option></select></label>
                    <label>Minimum observations<input id="api-min-count" type="number" min="0" value="0"></label>
                    <label>Minimum p95 (ms)<input id="api-min-p95" type="number" min="0" value="0"></label>
                    <label>Group depth<select id="api-depth"><option value="1">1 path segment</option><option value="2">2 path segments</option><option value="3">3 path segments</option></select></label>
                  </div>
                  <div id="api-groups" class="api-groups"></div>
                  <div id="api-list" hidden>
                    <div class="api-selection"><div><strong id="api-selection-count" aria-live="polite"></strong><small id="api-selection-scope"></small></div>
                      <div class="api-commands"><button type="button" id="api-top">Select top 20</button><button type="button" id="api-clear">Clear selection</button>
                        <button type="button" id="api-create">Create drafts</button><button type="button" id="api-export">Export draft bundle</button></div></div>
                    <div class="api-table-scroll" tabindex="0" role="region" aria-label="API statistics"><table class="api-table"><thead><tr>
                      <th><input type="checkbox" id="api-select-page" aria-label="Select current page"></th>
                      <th data-api-sort="endpoint" aria-sort="none"><button type="button">API</button></th>
                      <th data-api-sort="requestCount" aria-sort="descending"><button type="button">Observations</button></th>
                      <th>Share <span class="api-info" tabindex="0" title="Share of all retained HTTP observations, not business importance.">i</span></th>
                      <th data-api-sort="averageMillis" aria-sort="none"><button type="button">Average</button></th>
                      <th data-api-sort="p95Millis" aria-sort="none"><button type="button">p95</button></th>
                      <th data-api-sort="maximumMillis" aria-sort="none"><button type="button">Maximum</button></th>
                      <th data-api-sort="clientErrorCount" aria-sort="none"><button type="button">4xx</button></th>
                      <th data-api-sort="serverErrorCount" aria-sort="none"><button type="button">5xx</button></th>
                    </tr></thead><tbody id="api-rows"></tbody></table></div>
                  </div>
                  <div class="api-pagination"><span id="api-page-label" aria-live="polite"></span><div><button type="button" id="api-prev" aria-label="Previous page">&larr;</button><button type="button" id="api-next" aria-label="Next page">&rarr;</button></div></div>
                  <p id="api-empty" hidden>No APIs match these filters.</p>
                  <p id="api-export-status" role="status"></p>
                  <details class="api-measurement"><summary>Measurement and draft scope</summary>
                    <p>Intervals are server-adapter processing time, not network latency. Unknown or unfinished statuses are not successes. A 4xx can be expected by a test.</p>
                    <p>API statistics cover all retained observations. Draft sources are limited to the latest 100 HTTP details. Missing sources are listed in the bundle; no request input or assertion is invented. A draft is not replayable or automatically executed.</p>
                    <p>Group averages are weighted by timed samples. Group p95 is not calculated from API percentiles. Fewer than 20 timed samples is a caution, not a confidence threshold.</p>
                  </details>
                </section>
                """;
    }
}

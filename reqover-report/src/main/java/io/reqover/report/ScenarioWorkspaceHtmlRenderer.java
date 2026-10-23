/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.report;

final class ScenarioWorkspaceHtmlRenderer {
    private ScenarioWorkspaceHtmlRenderer() { }
    static String render() {
        return """
                <section class="section" id="scenario-workspace" hidden>
                  <h2 class="diagnostics-title">Scenarios and runs</h2>
                  <div class="map-modes" role="group" aria-label="Scenario workspace view"><button type="button" data-scenario-view="cases" aria-pressed="true">Scenario</button><button type="button" data-scenario-view="runs" aria-pressed="false">Run history</button><button type="button" data-scenario-view="recordings" aria-pressed="false">Recordings</button></div>
                  <p id="scenario-message" role="status"></p>
                  <div id="scenario-cases-panel">
                  <div class="workspace-panel">
                  <div class="panel-heading"><h3>Scenario details</h3><p>Save a set of checks or import an existing scenario to review.</p></div>
                  <div class="scenario-fields">
                    <label>Scenario name<input id="scenario-name" value="Regression checks" maxlength="120"></label>
                    <label>Environment<input id="scenario-environment" value="local" maxlength="80"></label>
                    <label>Code revision<input id="scenario-revision" maxlength="120"></label>
                  </div>
                  <div class="scenario-toolbar">
                    <button type="button" id="scenario-save" class="control-primary">Save scenario</button>
                """ + DiagnosticDashboard.filePicker("scenario-import", "Import scenario", false) + """
                    <button type="button" id="scenario-review">Review cases</button>
                  </div>
                  <p id="scenario-count" class="scenario-ready-bar" role="status"></p>
                  </div>
                  <div class="workspace-panel">
                  <div class="panel-heading"><h3>Export reviewed cases</h3><p>Download a collection, a disabled test plan or a command for a local target.</p></div>
                  <div class="scenario-toolbar">
                    <button type="button" id="scenario-postman">Export Postman</button>
                    <button type="button" id="scenario-jmeter">Export disabled JMeter plan</button>
                    <button type="button" id="scenario-command">Download local run command</button>
                  </div>
                  <p id="scenario-validation" class="case-problems" role="status"></p>
                  </div>
                  </div>
                  <div id="scenario-runs-panel" hidden>
                  <div class="scenario-history-heading"><h3>Loaded run history</h3><span id="scenario-history-count"></span></div>
                  <div class="scenario-toolbar">
                """ + DiagnosticDashboard.filePicker("scenario-runs-import", "Import runs / history", true) + """
                    <button type="button" id="scenario-history-save">Save loaded history</button>
                    <label class="scenario-search">Search history<input type="search" id="scenario-history-filter" placeholder="Name, environment or revision"></label>
                  </div>
                  <div class="scenario-scroll" tabindex="0" role="region" aria-label="Loaded run history"><table class="scenario-table"><thead><tr><th>Scenario / environment / revision</th><th>Started UTC</th><th>Passed / failed / not run</th><th>Client avg / p95</th><th>Actions</th></tr></thead><tbody id="scenario-history-rows"></tbody></table></div>
                  <div class="scenario-fields">
                    <label>Current run<select id="scenario-current"></select></label>
                    <label>Baseline run<select id="scenario-baseline"></select></label>
                  </div>
                  <div id="scenario-run-summary" class="api-totals"></div>
                  <ul id="scenario-comparison-warnings" class="case-problems"></ul>
                  <div class="scenario-scroll" tabindex="0" role="region" aria-label="Run comparison"><table class="scenario-table"><thead><tr><th>Metric</th><th>Baseline</th><th>Current</th></tr></thead><tbody id="scenario-comparison-rows"></tbody></table></div>
                  <h3>Current attempt details</h3><p id="scenario-attempt-count" class="graph-legend"></p>
                  <div class="scenario-scroll" tabindex="0" role="region" aria-label="Client attempt results"><table class="scenario-table"><thead><tr><th>Case</th><th>Actual HTTP</th><th>Client elapsed</th><th>Assertion outcome</th></tr></thead><tbody id="scenario-attempt-rows"></tbody></table></div>
                  </div>
                  <div id="scenario-recordings-panel" hidden><div id="recording-history-panel">
                    <div class="scenario-history-heading"><h3>Recording archive: adapter observations</h3><span id="recording-count"></span></div>
                    <div class="scenario-toolbar">
                """ + DiagnosticDashboard.filePicker("recording-import", "Import recording / catalog", true) + """
                      <button type="button" id="recording-save">Save loaded catalog</button>
                    </div>
                    <p id="recording-message" role="status"></p>
                    <div class="scenario-scroll" tabindex="0" role="region" aria-label="Recording archive"><table class="scenario-table"><thead><tr><th>Name / environment / revision</th><th>Recorded UTC</th><th>Detail boundary</th><th>Actions</th></tr></thead><tbody id="recording-rows"></tbody></table></div>
                    <div class="scenario-fields"><label>Current recording<select id="recording-current"></select></label><label>Baseline recording<select id="recording-baseline"></select></label></div>
                    <ul id="recording-warnings" class="case-problems"></ul>
                    <div class="scenario-scroll" tabindex="0" role="region" aria-label="Recording interval comparison"><table class="scenario-table"><thead><tr><th>API (first 100)</th><th>Adapter average delta</th><th>Adapter p95 delta</th><th>HTTP error delta</th></tr></thead><tbody id="recording-comparison"></tbody></table></div>
                  </div></div>
                </section>
                """;
    }
}

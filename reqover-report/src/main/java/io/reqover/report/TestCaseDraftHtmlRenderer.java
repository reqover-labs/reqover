/*
 * Copyright 2026 Reqover contributors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.reqover.report;

/** Offline editor for explicitly reviewed drafts, not automatic request replay. */
final class TestCaseDraftHtmlRenderer {
    private TestCaseDraftHtmlRenderer() {
    }

    static String styles() {
        return DiagnosticDashboard.asset("test-case-drafts.css");
    }

    static String render(boolean hasRequests) {
        if (!hasRequests) {
            return "";
        }
        return """
                <section class="section" id="test-case-drafts" hidden>
                  <div class="case-heading"><h2 class="diagnostics-title">Test drafts</h2>
                    <span id="reqover-case-count" class="graph-legend">0 drafts</span></div>
                  <p id="reqover-case-empty" class="empty">No test drafts.</p>
                  <div class="case-workspace" id="reqover-case-workspace" hidden>
                    <aside class="case-list" id="reqover-case-list" aria-label="Test drafts"></aside>
                    <form id="reqover-case-form" class="case-editor">
                      <div class="case-heading"><h3 id="reqover-case-title"></h3>
                        <span id="reqover-case-state" class="case-state">Review required</span></div>
                      <dl class="case-source"><dt>Source request</dt><dd id="reqover-case-source"></dd>
                        <dt>Observed status</dt><dd id="reqover-case-observed"></dd>
                        <dt>Recorded interval</dt><dd id="reqover-case-interval"></dd>
                        <dt>Original inputs</dt><dd>Not collected</dd></dl>
                      <div class="case-inputs">
                        <label for="reqover-case-path">Reviewed path
                          <input id="reqover-case-path" type="text" autocomplete="off" spellcheck="false" placeholder="/orders/42" required>
                        </label>
                        <label for="reqover-case-expected-status">Expected HTTP status
                          <input id="reqover-case-expected-status" type="number" min="200" max="599" step="1" required>
                        </label>
                        <label for="reqover-case-max-ms">Maximum client time (ms)
                          <input id="reqover-case-max-ms" type="number" min="1" max="600000" step="1" placeholder="Not set">
                        </label>
                      </div>
                      <label class="case-review" for="reqover-case-reviewed">
                        <input id="reqover-case-reviewed" type="checkbox">Test path, target and required inputs reviewed
                      </label>
                      <ul id="reqover-case-problems" class="case-problems" aria-live="polite"></ul>
                      <div class="case-actions">
                """ + download("reqover-case-json", "Download JSON draft")
                + download("reqover-case-junit", "Download JUnit draft") + """
                      </div>
                      <details class="case-code"><summary>JUnit draft <span class="graph-legend">Disabled by default</span></summary>
                        <pre><code id="reqover-case-preview"></code></pre>
                      </details>
                    </form>
                  </div>
                </section>
                """;
    }

    static String script() {
        return "<script>" + DiagnosticDashboard.asset("test-case-drafts.js") + "</script>\n"
                + "<script>" + DiagnosticDashboard.asset("test-case-editor.js") + "</script>\n";
    }

    private static String download(String id, String label) {
        return "<button type=\"button\" id=\"" + id + "\" class=\"case-command\">"
                + DiagnosticDashboard.icon("download") + "<span>" + label + "</span></button>";
    }
}

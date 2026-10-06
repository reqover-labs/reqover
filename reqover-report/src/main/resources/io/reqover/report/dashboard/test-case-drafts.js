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

(function (root, factory) {
  if (typeof module === 'object' && module.exports) { module.exports = factory(); }
  else { root.ReqoverTestDrafts = factory(); }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  function create(request) {
    if (!request || request.unitType !== 'http-request') { throw new Error('An HTTP observation is required.'); }
    var match = /^([A-Z]+) (.+)$/.exec(request.endpoint);
    var code = new Set();
    (request.classes || []).forEach(function (type) {
      type.methods.forEach(function (method) { code.add(type.className + '#' + method.methodName + method.descriptor); });
    });
    return {
      schemaVersion: 1, kind: 'reqover-http-test-draft', state: 'review-required', replayable: false,
      source: {
        requestId: request.requestId, endpoint: request.endpoint, startedAt: request.startedAt,
        observedStatus: request.endedAt && request.statusCode >= 200 && request.statusCode <= 599 ? request.statusCode : null,
        recordedProcessingMillis: Number.isFinite(request.duration) && request.duration >= 0 ? request.duration : null
      },
      request: { method: match ? match[1] : null, path: '' },
      expected: { statusCode: null, maximumClientMillis: null }, reviewed: false,
      observedCode: Array.from(code).sort(),
      limitations: ['Original path values, query, body and authentication were not captured.',
        'This is a reviewed test draft, not faithful replay of the source request.',
        'Client timing assertions are independent of recorded adapter intervals.']
    };
  }

  function number(value) { return value === '' || value === null || value === undefined ? null : Number(value); }
  function update(draft, changes) {
    var next = Object.assign({}, draft, {
      request: { method: draft.request.method, path: String(changes.path || '') },
      expected: { statusCode: number(changes.expectedStatus), maximumClientMillis: number(changes.maximumClientMillis) },
      reviewed: changes.reviewed === true
    });
    next.state = problems(next).length ? 'review-required' : 'reviewed-draft';
    return next;
  }

  function validPath(path) {
    if (!path.startsWith('/') || path.startsWith('//')) { return false; }
    var decoded;
    try { decoded = decodeURIComponent(path); } catch (_) { return false; }
    return !decoded.startsWith('//') && !/[\s\\?#{}*<>"\[\]^`|\u0000-\u001f\u007f-\u009f]/.test(decoded)
      && !decoded.split('/').some(function (segment) { return segment === '.' || segment === '..'; });
  }

  function problems(draft) {
    var result = [];
    if (!['GET', 'HEAD'].includes(draft.request.method)) { result.push('JUnit export supports GET and HEAD only.'); }
    if (!validPath(draft.request.path)) { result.push('A concrete path without query, fragment or route placeholders is required.'); }
    var status = draft.expected.statusCode;
    if (!Number.isInteger(status) || status < 200 || status > 599) { result.push('Expected HTTP status must be an integer from 200 to 599.'); }
    var maximum = draft.expected.maximumClientMillis;
    if (maximum !== null && (!Number.isInteger(maximum) || maximum < 1 || maximum > 600000)) {
      result.push('Maximum client time must be 1-600000 ms or unset.');
    }
    if (!draft.reviewed) { result.push('Explicit review of the test target and required inputs is required.'); }
    return result;
  }

  // Recorded metadata is untrusted; use Java string literals, never comments or identifiers.
  function javaString(value) {
    var text = '"';
    for (var char of String(value)) {
      if (char === '"') { text += '\\"'; }
      else if (char === '\\') { text += '\\\\'; }
      else if (char === '\n') { text += '\\n'; }
      else if (char === '\r') { text += '\\r'; }
      else if (char === '\t') { text += '\\t'; }
      else if (char.charCodeAt(0) < 32 || char.charCodeAt(0) === 127) { text += '\\' + char.charCodeAt(0).toString(8).padStart(3, '0'); }
      else { text += char; }
    }
    return text + '"';
  }

  function junit(draft, className) {
    className = className || 'ReqoverRegressionTest';
    if (!/^Reqover(?:Regression|Case[1-9][0-9]*)Test$/.test(className)) { throw new Error('Invalid generated class name.'); }
    var issues = problems(draft);
    if (issues.length) { throw new Error('Draft review required: ' + issues.join(' ')); }
    var source = [
      'package io.reqover.generated;', '',
      'import java.net.URI;', 'import java.net.http.HttpClient;', 'import java.net.http.HttpRequest;',
      'import java.net.http.HttpResponse;', 'import java.time.Duration;', 'import java.util.Objects;',
      'import org.junit.jupiter.api.Disabled;', 'import org.junit.jupiter.api.Test;',
      'import static org.junit.jupiter.api.Assertions.*;', '',
      '@Disabled("Review local/QA target and inputs, then enable explicitly. Not a replay of captured inputs.")',
      'public class ' + className + ' {',
      '    private static final String SOURCE_REQUEST = ' + javaString(draft.source.requestId) + ';',
      '    private static final String SOURCE_ROUTE = ' + javaString(draft.source.endpoint) + ';',
      '    private static final String[] OBSERVED_CODE = {' + draft.observedCode.map(javaString).join(', ') + '};', '',
      '    @Test', '    void reviewedRequestMeetsExpectation() throws Exception {',
      '        URI base = URI.create(Objects.requireNonNull(System.getProperty("reqover.test.baseUrl"),',
      '                "Set an explicit local/QA base URL before enabling this test"));',
      '        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))',
      '                || base.getHost() == null || base.getUserInfo() != null',
      '                || base.getRawQuery() != null || base.getRawFragment() != null',
      '                || !(base.getPath().isEmpty() || "/".equals(base.getPath()))) {',
      '            throw new IllegalArgumentException("Base URL must be an HTTP(S) origin without credentials or a path");',
      '        }',
      '        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)',
      '                .connectTimeout(Duration.ofSeconds(5)).build();',
      '        HttpRequest request = HttpRequest.newBuilder(base.resolve(' + javaString(draft.request.path) + '))',
      '                .timeout(Duration.ofSeconds(' + Math.max(10, Math.ceil((draft.expected.maximumClientMillis || 0) / 1000)) + '))',
      '                .method(' + javaString(draft.request.method) + ', HttpRequest.BodyPublishers.noBody()).build();',
      '        long started = System.nanoTime();',
      '        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());',
      '        long elapsedNanos = System.nanoTime() - started;',
      '        assertEquals(' + draft.expected.statusCode + ', response.statusCode());'
    ];
    if (draft.expected.maximumClientMillis !== null) {
      source.push('        assertTrue(Duration.ofNanos(elapsedNanos).compareTo(Duration.ofMillis('
        + draft.expected.maximumClientMillis + 'L)) <= 0, "Client elapsed time exceeded the reviewed limit");');
    }
    source.push('    }', '}', '');
    return source.join('\n');
  }

  return { create: create, update: update, problems: problems, junit: junit };
});

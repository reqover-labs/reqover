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

const assert = require('node:assert/strict');
const test = require('node:test');
const compare = require('../reqover-report/src/main/resources/io/reqover/report/dashboard/recording-comparison.js');

const start = '2026-10-04T00:00:00.000000000Z';
const observation = overrides => ({ requestId: 'r1', unitType: 'http-request', endpoint: 'GET /orders/{id}',
  startedAt: start, endedAt: '2026-10-04T00:00:00.100000000Z', statusCode: 200, ...overrides });
const report = requests => ({ schemaVersion: 1, generatedAt: start, completedRequestCount: requests.length,
  endpoints: [], reverseIndex: [], requests });
const summary = requests => compare.fromReport(report(requests));

test('preserves submillisecond precision rather than rounding JS Date milliseconds', () => {
  const value = summary([observation({ startedAt: '2026-10-04T00:00:00.999900001Z',
    endedAt: '2026-10-04T00:00:01.000000009Z' })]);
  assert.equal(value.endpoints[0].averageMillis, 0.100008);
});

test('matches HTTP summary rules for unknown status, non-HTTP and invalid intervals', () => {
  const value = summary([observation(), observation({ statusCode: 503 }),
    observation({ statusCode: 200, endedAt: null }), observation({ statusCode: 0 }),
    observation({ unitType: 'scheduled-job', endpoint: 'daily' }),
    observation({ startedAt: '2026-10-04T00:00:01Z', endedAt: start, statusCode: 400 })]);
  const row = value.endpoints[0];
  assert.equal(value.httpRequestCount, 5);
  assert.equal(row.knownStatusCount, 3);
  assert.equal(row.clientErrorCount, 1);
  assert.equal(row.serverErrorCount, 1);
  assert.equal(row.timedRequestCount, 3);
  assert.equal(row.averageMillis, 100);
});

test('computes nearest-rank p95 over valid retained intervals', () => {
  const requests = Array.from({ length: 20 }, (_, i) => observation({ endedAt:
    '2026-10-04T00:00:00.' + String((i + 1) * 1000000).padStart(9, '0') + 'Z' }));
  const row = summary(requests).endpoints[0];
  assert.equal(row.p95Millis, 19);
  assert.equal(row.maximumMillis, 20);
  assert.equal(row.averageMillis, 10.5);
});

test('calculates descriptive deltas with independent final-status denominators', () => {
  const before = summary([observation({ statusCode: 503 }), observation({ statusCode: 0 })]);
  const after = summary([observation({ endedAt: '2026-10-04T00:00:00.050Z' })]);
  const value = compare.between(before, after);
  assert.equal(value.rows[0].averageDeltaMillis, -50);
  assert.equal(value.rows[0].p95DeltaMillis, -50);
  assert.equal(value.rows[0].failureDeltaPoints, -100);
  assert(value.warnings.includes('sample-counts-differ'));
  assert(value.warnings.includes('small-timing-samples'));
  assert(value.warnings.includes('unknown-statuses'));
  assert.equal(value.verdict, null);
});

test('does not zero-fill missing measurements or treat unseen endpoints as deleted', () => {
  const before = summary([observation({ endpoint: 'GET /old', endedAt: null, statusCode: 0 })]);
  const after = summary([observation({ endpoint: 'GET /new' })]);
  const result = compare.between(before, after);
  assert.equal(result.rows.find(r => r.endpoint === 'GET /old').presence, 'baseline-only');
  assert.equal(result.rows.find(r => r.endpoint === 'GET /new').presence, 'current-only');
  assert(result.rows.every(row => row.averageDeltaMillis === null && row.failureDeltaPoints === null));
});

test('legacy coverage remains unavailable, not a zero-error baseline', () => {
  const legacy = report([]); delete legacy.requests;
  const result = compare.between(compare.fromReport(legacy), summary([observation()]));
  assert(result.warnings.includes('baseline-measurements-unavailable'));
  assert.equal(result.rows[0].presence, 'baseline-unavailable');
  assert.equal(result.rows[0].failureDeltaPoints, null);
});

test('rejects non-report JSON, future schema and malformed calendar timestamps', () => {
  for (const input of [{ kind: 'reqover-http-test-draft', schemaVersion: 1 },
    { ...report([]), schemaVersion: 2 }, report([observation({ startedAt: '2026-02-30T00:00:00Z' })]),
    report([observation({ startedAt: 'not-a-date' })]), report([observation({ statusCode: 200.5 })])]) {
    assert.throws(() => compare.fromReport(input));
  }
});

test('round-trips a compact summary without copying request IDs or arbitrary secrets', () => {
  const value = summary([observation({ requestId: 'private-request-marker', authorization: 'private-secret-marker' })]);
  const json = JSON.stringify(value);
  assert(!json.includes('private-request-marker') && !json.includes('private-secret-marker'));
  assert.deepEqual(compare.read(JSON.parse(json)), value);
});

test('rejects corrupt summary counts and zero-filled missing timing', () => {
  const value = summary([observation()]);
  const bad = structuredClone(value); bad.endpoints[0].knownStatusCount = 500;
  assert.throws(() => compare.read(bad));
  const noTime = summary([observation({ endedAt: null })]);
  noTime.endpoints[0].averageMillis = 0;
  assert.throws(() => compare.read(noTime));
  const wrongTime = structuredClone(value); wrongTime.endpoints[0].p95Millis = 500;
  assert.throws(() => compare.read(wrongTime));
  const wrongTotal = structuredClone(value); wrongTotal.endpoints[0].cumulativeMillis = 500;
  assert.throws(() => compare.read(wrongTotal));
});

test('honors bounds on input records and stable alphabetical endpoint identity', () => {
  assert.throws(() => compare.fromReport(report(Array(50001).fill(observation()))));
  const value = summary([observation({ endpoint: '__proto__' }), observation({ endpoint: 'GET /a' })]);
  assert.equal(value.endpoints.length, 2);
  assert.deepEqual(value.endpoints.map(row => row.endpoint), ['GET /a', '__proto__']);
});

test('ignores arithmetic noise when comparing equivalent recordings', () => {
  const before = summary([observation()]);
  const after = structuredClone(before);
  after.endpoints[0].averageMillis += 2.27e-13;
  const row = compare.between(before, after).rows[0];
  assert.equal(row.averageDeltaMillis, 0);
  after.endpoints[0].averageMillis += 0.000001;
  after.endpoints[0].cumulativeMillis = after.endpoints[0].averageMillis;
  after.endpoints[0].maximumMillis = after.endpoints[0].averageMillis;
  assert(compare.between(before, after).rows[0].averageDeltaMillis > 0);
});

test('treats missing endedAt as unfinished instead of throwing', () => {
  const request = observation(); delete request.endedAt;
  const row = summary([request]).endpoints[0];
  assert.equal(row.timedRequestCount, 0);
  assert.equal(row.knownStatusCount, 0);
  assert.equal(row.averageMillis, null);
});

test('does not compare a truncated detail export as a complete recording', () => {
  const truncated = report([observation()]); truncated.omittedRequestDetails = 50;
  assert.throws(() => compare.fromReport(truncated), /summary|truncated/i);
});

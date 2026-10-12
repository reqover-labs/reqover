// SPDX-License-Identifier: Apache-2.0
const test = require('node:test');
const assert = require('node:assert/strict');
const api = require('../reqover-report/src/main/resources/io/reqover/report/dashboard/api-workspace.js');
const row = (endpoint, count, time, errors = 0) => ({ endpoint, requestCount: count, knownStatusCount: count,
  timedRequestCount: time === null ? 0 : count, averageMillis: time, p95Millis: time, maximumMillis: time,
  cumulativeMillis: time === null ? 0 : count * time, clientErrorCount: 0, serverErrorCount: errors });
const rows = [row('GET /orders/{id}', 100, 10), row('POST /orders', 1, 1000, 1), row('GET /health', 2, null)];
test('selection summary exposes missing sources and hidden selections before actions', () => {
  const selected = new Set(['GET /orders/{id}', 'POST /orders']);
  const state = api.selectionSummary(rows, selected, [rows[0]], [{ endpoint: 'GET /orders/{id}', requestId: 'r1', startedAt: '2026-10-12T00:00:00Z' }]);
  assert.deepEqual(state, { selectedCount: 2, observationCount: 101, hiddenCount: 1, availableCount: 1, missingCount: 1 });
});
test('path groups combine counts and weighted timing, never average percentiles', () => {
  const groups = api.groups(rows);
  const orders = groups.find(g => g.name === '/orders');
  assert.equal(orders.requestCount, 101);
  assert.equal(orders.averageMillis, 2000 / 101);
  assert.equal(orders.serverErrorCount, 1);
  assert(!Object.hasOwn(orders, 'p95Millis'));
  assert.equal(groups.find(g => g.name === '/health').averageMillis, null);
});
test('queries and route parameters do not create extra path groups', () => {
  assert.equal(api.groupName('GET /orders/{id}?secret=value'), '/orders');
  assert.equal(api.groupName('GET /'), '/');
  assert.equal(api.groupName('GET /{tenant}/orders'), '/{tenant}');
  assert.equal(api.groupName('GET /api/v1/orders', 2), '/api/v1');
});
test('representative selection compares instants rather than variable-length timestamp strings', () => {
  const requests = [
    { endpoint: 'GET /a', requestId: 'seconds', startedAt: '2026-10-12T10:00:00Z' },
    { endpoint: 'GET /a', requestId: 'nano', startedAt: '2026-10-12T10:00:00.000000002Z' },
    { endpoint: 'GET /a', requestId: 'older-nano', startedAt: '2026-10-12T10:00:00.000000001Z' }
  ];
  assert.equal(api.sources(new Set(['GET /a']), requests).requests[0].requestId, 'nano');
});
test('filter and sort are stable, nonmutating, and missing timing is not zero', () => {
  assert.deepEqual(api.filter(rows, { search: 'orders', status: '5xx', metric: 'requestCount', direction: 'desc' }).map(r => r.endpoint), ['POST /orders']);
  assert.equal(api.filter(rows, { metric: 'p95Millis', direction: 'asc' }).at(-1).endpoint, 'GET /health');
  assert.equal(api.filter(rows, { search: '', minCount: 50 }).length, 1);
  assert.equal(rows[0].endpoint, 'GET /orders/{id}');
});
test('batch uses latest available detail and explicitly reports missing sources', () => {
  const requests = [{ endpoint: 'GET /orders/{id}', requestId: 'new', startedAt: '2026-10-12T10:00:00Z' },
    { endpoint: 'GET /orders/{id}', requestId: 'old', startedAt: '2026-10-12T09:00:00Z' }];
  const batch = api.sources(new Set(['GET /orders/{id}', 'POST /orders']), requests);
  assert.equal(batch.requests[0].requestId, 'new');
  assert.deepEqual(batch.missing, ['POST /orders']);
  assert.equal(requests.length, 2);
});

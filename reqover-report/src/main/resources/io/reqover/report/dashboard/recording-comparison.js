(function (root, factory) {
  if (typeof module === 'object' && module.exports) { module.exports = factory(); }
  else { root.ReqoverRecordingComparison = factory(); }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';
  var KIND = 'reqover-recorded-summary';
  var LIMIT = 50000;

  function instant(text) {
    if (typeof text !== 'string') { throw new Error('Recorded timestamps must be ISO instants.'); }
    var match = /^([+-]?\d{4,9})(-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))?Z$/.exec(text);
    if (!match) { throw new Error('Unsupported recorded timestamp.'); }
    var year = Number(match[1]);
    var prefix = year >= 0 && year <= 9999 ? String(year).padStart(4, '0')
      : (year < 0 ? '-' : '+') + String(Math.abs(year)).padStart(6, '0');
    var seconds = prefix + match[2] + 'Z';
    var millis = Date.parse(seconds);
    if (!Number.isFinite(millis) || new Date(millis).toISOString().replace('.000Z', 'Z') !== seconds) {
      throw new Error('Invalid or unsupported calendar timestamp.');
    }
    return BigInt(millis) * 1000000n + BigInt((match[3] || '').padEnd(9, '0'));
  }
  function finite(value) { return typeof value === 'number' && Number.isFinite(value) && value >= 0; }
  function count(value) { return Number.isSafeInteger(value) && value >= 0; }
  function order(a, b) { return a.endpoint < b.endpoint ? -1 : a.endpoint > b.endpoint ? 1 : 0; }

  function fromReport(report) {
    if (!report || !count(report.completedRequestCount) || typeof report.generatedAt !== 'string'
      || !Array.isArray(report.endpoints) || !Array.isArray(report.reverseIndex)) { throw new Error('Not a Reqover coverage report.'); }
    if (report.schemaVersion !== undefined && report.schemaVersion !== 1) { throw new Error('Unsupported report schema; use schema 1.'); }
    instant(report.generatedAt);
    if (report.requests !== undefined && report.requests !== null && !Array.isArray(report.requests)) { throw new Error('Invalid request observations.'); }
    var requests = report.requests || [];
    if (requests.length > LIMIT) { throw new Error('At most 50000 retained observations can be imported.'); }
    var grouped = new Map();
    requests.forEach(function (request) {
      if (!request || typeof request.unitType !== 'string') { throw new Error('Invalid observation type.'); }
      if (request.unitType !== 'http-request') { return; }
      if (typeof request.endpoint !== 'string' || !Number.isInteger(request.statusCode)) { throw new Error('Invalid HTTP observation.'); }
      var start = instant(request.startedAt);
      var end = request.endedAt === null ? null : instant(request.endedAt);
      var group = grouped.get(request.endpoint);
      if (!group) {
        group = { endpoint: request.endpoint, requestCount: 0, knownStatusCount: 0,
          clientErrorCount: 0, serverErrorCount: 0, times: [] };
        grouped.set(request.endpoint, group);
      }
      group.requestCount++;
      if (end !== null && request.statusCode >= 200 && request.statusCode <= 599) {
        group.knownStatusCount++;
        if (request.statusCode >= 500) { group.serverErrorCount++; }
        else if (request.statusCode >= 400) { group.clientErrorCount++; }
      }
      if (end !== null && end >= start) { group.times.push(Number(end - start) / 1000000); }
    });
    var endpoints = Array.from(grouped.values()).map(function (group) {
      group.times.sort(function (a, b) { return a - b; });
      var n = group.times.length;
      var total = group.times.reduce(function (a, b) { return a + b; }, 0);
      return { endpoint: group.endpoint, requestCount: group.requestCount, knownStatusCount: group.knownStatusCount,
        clientErrorCount: group.clientErrorCount, serverErrorCount: group.serverErrorCount, timedRequestCount: n,
        averageMillis: n ? total / n : null, p95Millis: n ? group.times[Math.ceil(n * 0.95) - 1] : null,
        maximumMillis: n ? group.times[n - 1] : null, cumulativeMillis: total };
    }).sort(order);
    var total = endpoints.reduce(function (n, row) { return n + row.requestCount; }, 0);
    return { schemaVersion: 1, kind: KIND, generatedAt: report.generatedAt,
      available: total > 0, httpRequestCount: total, endpoints: endpoints };
  }

  function read(value) {
    if (!value || value.kind !== KIND) { return fromReport(value); }
    if (value.schemaVersion !== 1 || !count(value.httpRequestCount) || typeof value.available !== 'boolean'
      || !Array.isArray(value.endpoints) || value.endpoints.length > LIMIT) { throw new Error('Invalid recording summary.'); }
    instant(value.generatedAt);
    var seen = new Set();
    var endpoints = value.endpoints.map(function (row) {
      if (!row || typeof row.endpoint !== 'string' || seen.has(row.endpoint)) { throw new Error('Invalid or duplicate endpoint summary.'); }
      seen.add(row.endpoint);
      var fields = ['requestCount', 'knownStatusCount', 'clientErrorCount', 'serverErrorCount', 'timedRequestCount'];
      if (!fields.every(function (field) { return count(row[field]); })
        || row.knownStatusCount > row.requestCount || row.timedRequestCount > row.requestCount
        || row.clientErrorCount + row.serverErrorCount > row.knownStatusCount || !finite(row.cumulativeMillis)) {
        throw new Error('Invalid summary counts.');
      }
      var timing = ['averageMillis', 'p95Millis', 'maximumMillis'];
      if (!timing.every(function (field) { return row.timedRequestCount ? finite(row[field]) : row[field] === null; })
        || (!row.timedRequestCount && row.cumulativeMillis !== 0)) { throw new Error('Invalid or zero-filled missing timing.'); }
      if (row.timedRequestCount) {
        var tolerance = Math.max(1e-9, row.cumulativeMillis * 1e-12);
        if (row.averageMillis > row.maximumMillis + tolerance || row.p95Millis > row.maximumMillis + tolerance
          || Math.abs(row.averageMillis * row.timedRequestCount - row.cumulativeMillis) > tolerance) {
          throw new Error('Inconsistent timing summary.');
        }
      }
      var clean = { endpoint: row.endpoint };
      fields.concat(timing, ['cumulativeMillis']).forEach(function (field) { clean[field] = row[field]; });
      return clean;
    }).sort(order);
    var total = endpoints.reduce(function (n, row) { return n + row.requestCount; }, 0);
    if (total !== value.httpRequestCount || value.available !== (total > 0)) { throw new Error('Inconsistent recording summary.'); }
    return { schemaVersion: 1, kind: KIND, generatedAt: value.generatedAt,
      available: value.available, httpRequestCount: total, endpoints: endpoints };
  }

  function failures(row) {
    return row && row.knownStatusCount ? (row.clientErrorCount + row.serverErrorCount) * 100 / row.knownStatusCount : null;
  }
  function delta(before, after) { return before !== null && after !== null ? after - before : null; }
  function between(before, after) {
    before = read(before); after = read(after);
    var b = new Map(before.endpoints.map(function (row) { return [row.endpoint, row]; }));
    var a = new Map(after.endpoints.map(function (row) { return [row.endpoint, row]; }));
    var warnings = new Set(['conditions-not-recorded']);
    if (!before.available) { warnings.add('baseline-measurements-unavailable'); }
    if (!after.available) { warnings.add('current-measurements-unavailable'); }
    var names = Array.from(new Set(Array.from(b.keys()).concat(Array.from(a.keys())))).sort();
    var rows = names.map(function (endpoint) {
      var baseline = b.get(endpoint) || null; var current = a.get(endpoint) || null;
      if (baseline && current && (baseline.requestCount !== current.requestCount || baseline.timedRequestCount !== current.timedRequestCount)) {
        warnings.add('sample-counts-differ');
      }
      [baseline, current].forEach(function (row) {
        if (!row) { return; }
        if (row.timedRequestCount < 20) { warnings.add('small-timing-samples'); }
        if (row.knownStatusCount < row.requestCount) { warnings.add('unknown-statuses'); }
      });
      var presence = !before.available ? 'baseline-unavailable' : !after.available ? 'current-unavailable'
        : baseline && current ? 'both' : baseline ? 'baseline-only' : 'current-only';
      return { endpoint: endpoint, baseline: baseline, current: current, presence: presence,
        averageDeltaMillis: delta(baseline ? baseline.averageMillis : null, current ? current.averageMillis : null),
        p95DeltaMillis: delta(baseline ? baseline.p95Millis : null, current ? current.p95Millis : null),
        failureDeltaPoints: delta(failures(baseline), failures(current)) };
    });
    return { baseline: before, current: after, rows: rows, warnings: Array.from(warnings), verdict: null };
  }
  return { fromReport: fromReport, read: read, between: between, failurePercent: failures };
});

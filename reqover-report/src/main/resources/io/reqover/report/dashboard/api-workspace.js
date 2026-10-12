/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) { module.exports = factory(); }
  else { root.ReqoverApiWorkspace = factory(); }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';
  function groupName(endpoint, depth) {
    var path = endpoint.slice(endpoint.indexOf(' ') + 1).split('?')[0];
    return '/' + path.split('/').filter(Boolean).slice(0, depth || 1).join('/');
  }
  function groups(rows, depth) {
    var map = new Map();
    rows.forEach(function (row) {
      var name = groupName(row.endpoint, depth);
      if (!map.has(name)) { map.set(name, { name: name, endpoints: [], requestCount: 0, knownStatusCount: 0,
        clientErrorCount: 0, serverErrorCount: 0, timedRequestCount: 0, cumulativeMillis: 0 }); }
      var g = map.get(name); g.endpoints.push(row.endpoint);
      ['requestCount', 'knownStatusCount', 'clientErrorCount', 'serverErrorCount', 'timedRequestCount', 'cumulativeMillis'].forEach(function (key) { g[key] += row[key]; });
    });
    return Array.from(map.values()).map(function (g) { g.averageMillis = g.timedRequestCount ? g.cumulativeMillis / g.timedRequestCount : null; return g; })
      .sort(function (a, b) { return b.requestCount - a.requestCount || a.name.localeCompare(b.name); });
  }
  function filter(rows, options) {
    options = options || {};
    var search = (options.search || '').toLowerCase();
    var metric = options.metric || 'requestCount', sign = options.direction === 'asc' ? 1 : -1;
    return rows.filter(function (r) {
      return r.endpoint.toLowerCase().includes(search) && r.requestCount >= (options.minCount || 0)
        && (!options.group || groupName(r.endpoint, options.depth) === options.group)
        && (!(options.minP95 > 0) || (r.p95Millis !== null && r.p95Millis >= options.minP95))
        && (!options.status || options.status === 'all' || (options.status === '5xx' ? r.serverErrorCount : r.clientErrorCount) > 0);
    }).sort(function (a, b) {
      if (metric === 'endpoint') { return sign * a.endpoint.localeCompare(b.endpoint); }
      if (a[metric] === null && b[metric] !== null) { return 1; }
      if (b[metric] === null && a[metric] !== null) { return -1; }
      return sign * (a[metric] - b[metric]) || a.endpoint.localeCompare(b.endpoint);
    });
  }
  function sources(selected, requests) {
    var latest = new Map();
    function nanos(text) { var match = /\.(\d{1,9})Z$/.exec(text); return Number((match ? match[1] : '').padEnd(9, '0')); }
    function newer(a, b) { return Date.parse(a) > Date.parse(b) || (Date.parse(a) === Date.parse(b) && nanos(a) > nanos(b)); }
    requests.forEach(function (r) { if (selected.has(r.endpoint) && (!latest.has(r.endpoint) || newer(r.startedAt, latest.get(r.endpoint).startedAt))) { latest.set(r.endpoint, r); } });
    return { requests: Array.from(selected).filter(function (e) { return latest.has(e); }).map(function (e) { return latest.get(e); }),
      missing: Array.from(selected).filter(function (e) { return !latest.has(e); }) };
  }
  function selectionSummary(rows, selected, visible, requests) {
    var visibleNames = new Set(visible.map(function (r) { return r.endpoint; }));
    var source = sources(selected, requests);
    return { selectedCount: selected.size,
      observationCount: rows.reduce(function (n, r) { return n + (selected.has(r.endpoint) ? r.requestCount : 0); }, 0),
      hiddenCount: Array.from(selected).filter(function (name) { return !visibleNames.has(name); }).length,
      availableCount: source.requests.length, missingCount: source.missing.length };
  }
  return { groupName: groupName, groups: groups, filter: filter, sources: sources, selectionSummary: selectionSummary };
});

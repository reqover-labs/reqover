/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
(function () {
  'use strict';
  var section = document.getElementById('api-workspace');
  if (!section) { return; }
  var api = window.ReqoverApiWorkspace;
  var summary = JSON.parse(document.getElementById('reqover-comparison-data').textContent);
  var rows = summary.endpoints;
  var requests = JSON.parse(document.getElementById('reqover-map-data').textContent).requests || [];
  var details = Array.from(document.querySelectorAll('details.request-detail'));
  requests.forEach(function (r, i) { var value = details[i] && details[i].dataset.requestDuration; r.duration = value ? Number(value) : null; });
  var selected = new Set(), page = 0, size = 20, mode = api.initialView(rows.length, summary.httpRequestCount), group = '', depth = 1;
  var scope = null;
  var distribution = JSON.parse(document.getElementById('reqover-time-data').textContent);
  var metric = 'requestCount', direction = 'desc', visible = [];
  var pages = { distribution: 0, groups: 0, table: 0 };
  function node(tag, text, cls) { var e = document.createElement(tag); if (text !== undefined) { e.textContent = text; } if (cls) { e.className = cls; } return e; }
  function byId(id) { return document.getElementById('api-' + id); }
  function time(value) { return value === null ? 'Not timed' : value.toFixed(2) + ' ms'; }
  function percent(n, total) { return total ? (100 * n / total).toFixed(1) + '%' : 'Not available'; }
  function stat(parent, label, value) { var item = node('div'); item.append(node('span', label), node('strong', value)); parent.append(item); }
  var total = summary.httpRequestCount;
  function renderTotals() {
    byId('totals').replaceChildren();
    stat(byId('totals'), 'Observed APIs', rows.length);
    stat(byId('totals'), scope ? 'Selected observations' : 'Retained observations', total.toLocaleString());
    stat(byId('totals'), 'HTTP 4xx', rows.reduce(function (n, r) { return n + r.clientErrorCount; }, 0).toLocaleString());
    stat(byId('totals'), 'HTTP 5xx', rows.reduce(function (n, r) { return n + r.serverErrorCount; }, 0).toLocaleString());
  }
  function scopedRequests() { return requests.filter(function (r) { return api.matchesScope(r, scope); }); }
  var markTime = window.ReqoverTimeView.init(distribution, function (nextScope, nextRows) {
    scope = nextScope; rows = nextRows; total = rows.reduce(function (n, r) { return n + r.requestCount; }, 0);
    selected.clear(); resetFilters(); markTime(scope); renderTotals();
    if (mode === 'groups') { render(); } else { switchMode('groups'); }
  });
  byId('time-clear').addEventListener('click', function () {
    scope = null; rows = summary.endpoints; total = summary.httpRequestCount;
    selected.clear(); resetFilters(); markTime(null); renderTotals(); render();
  });

  function options() { return { search: byId('search').value, status: byId('status').value,
    minCount: Math.max(0, Number(byId('min-count').value) || 0), minP95: Math.max(0, Number(byId('min-p95').value) || 0),
    group: group, depth: depth, metric: metric, direction: direction }; }
  function renderSelection() {
    var state = api.selectionSummary(rows, selected, visible, scopedRequests());
    byId('selection-bar').hidden = mode !== 'table' && selected.size === 0;
    byId('selection-count').textContent = selected.size + ' selected APIs / ' + state.observationCount.toLocaleString() + ' observations';
    byId('selection-scope').textContent = percent(state.observationCount, total) + ' of retained observations' + (state.hiddenCount ? ' / ' + state.hiddenCount + ' outside current filters' : '');
    byId('source-count').textContent = state.availableCount + ' available draft sources / ' + state.missingCount + ' without recent detail';
    byId('export').disabled = selected.size === 0; byId('clear').disabled = selected.size === 0;
    byId('create').disabled = state.availableCount === 0;
  }
  function switchMode(next) {
    if (next === mode) { return; }
    pages[mode] = page;
    mode = next;
    if (next === 'groups') { group = ''; }
    document.querySelectorAll('[data-api-view]').forEach(function (b) { b.setAttribute('aria-pressed', String(b.dataset.apiView === next)); });
    page = pages[next]; render();
  }
  function render() {
    byId('distribution').hidden = mode !== 'distribution';
    byId('toolbar').hidden = mode === 'distribution';
    document.querySelector('.api-context').hidden = mode === 'distribution';
    byId('time-clear').hidden = !scope;
    byId('time-label').textContent = scope ? scope.startedAt + ' to ' + scope.endedAt + ' / ' + (scope.band === null ? 'All intervals' : distribution.windows.find(function (w) { return w.startedAt === scope.startedAt; }).bands[scope.band].label) : 'All recorded time';
    document.querySelectorAll('[data-api-view]').forEach(function (b) { b.setAttribute('aria-pressed', String(b.dataset.apiView === mode)); });
    visible = api.filter(rows, options());
    byId('group-label').textContent = group ? 'Path group: ' + group : 'All path groups';
    byId('group-clear').hidden = !group;
    var groupRows = api.groups(visible, depth);
    var itemCount = mode === 'groups' ? groupRows.length : visible.length;
    page = Math.min(page, Math.max(0, Math.ceil(itemCount / size) - 1));
    byId('groups').hidden = mode !== 'groups'; byId('list').hidden = mode !== 'table';
    byId('empty').hidden = mode === 'distribution' || visible.length > 0;
    byId('empty').textContent = rows.length ? 'No APIs match these filters.' : 'No retained HTTP diagnostics in this recording. Record HTTP requests with a current adapter to use this workspace.';
    byId('groups').replaceChildren();
    if (mode === 'groups') {
      groupRows.slice(page * size, (page + 1) * size).forEach(function (g) {
        var b = node('button', undefined, 'api-group'); b.type = 'button';
        b.append(node('h3', g.name), node('small', g.endpoints.length + ' APIs / ' + percent(g.requestCount, total) + ' of retained traffic'));
        var metrics = node('div', undefined, 'api-group-metrics');
        [['Observations', g.requestCount.toLocaleString()], ['Weighted average', time(g.averageMillis)], ['HTTP 4xx / 5xx', g.clientErrorCount + ' / ' + g.serverErrorCount], ['Timed samples', g.timedRequestCount.toLocaleString()]].forEach(function (pair) {
          var cell = node('div'); cell.append(node('span', pair[0]), node('b', pair[1])); metrics.append(cell);
        });
        var track = node('div', undefined, 'api-share-track'), fill = node('span'); fill.style.width = (total ? 100 * g.requestCount / total : 0) + '%'; track.append(fill);
        b.append(metrics, track); b.addEventListener('click', function () { group = g.name; pages.table = 0; switchMode('table'); }); byId('groups').append(b);
      });
    }
    var slice = visible.slice(page * size, (page + 1) * size);
    byId('rows').replaceChildren();
    slice.forEach(function (r) {
      var tr = node('tr'); tr.dataset.selected = String(selected.has(r.endpoint));
      var check = node('input'); check.type = 'checkbox'; check.checked = selected.has(r.endpoint); check.setAttribute('aria-label', 'Select ' + r.endpoint);
      check.addEventListener('change', function () {
        if (check.checked) { selected.add(r.endpoint); } else { selected.delete(r.endpoint); }
        render();
        var replacement = Array.from(byId('rows').querySelectorAll('input')).find(function (input) { return input.getAttribute('aria-label') === 'Select ' + r.endpoint; });
        if (replacement) { replacement.focus({ preventScroll: true }); }
      });
      var first = node('td'); first.append(check); tr.append(first);
      var name = node('td'), open = node('button', r.endpoint, 'api-endpoint'); open.type = 'button'; open.title = 'Show observed code';
      open.addEventListener('click', function () { window.ReqoverFilter.selectEndpoint(r.endpoint); window.location.hash = 'endpoint-code'; });
      name.append(open, node('small', r.timedRequestCount + ' timed / ' + (r.requestCount - r.knownStatusCount) + ' unknown statuses' + (r.timedRequestCount < 20 ? ' / small timing sample' : ''))); tr.append(name);
      var representative = api.sources(new Set([r.endpoint]), scopedRequests()).requests[0];
      var requestLink = node('button', 'Request map', 'api-request-link'); requestLink.type = 'button'; requestLink.disabled = !representative;
      requestLink.title = representative ? 'Open the latest retained request in this selected scope' : 'No recent request detail in the selected scope';
      requestLink.addEventListener('click', function () {
        if (representative) { window.dispatchEvent(new CustomEvent('reqover-open-request', { detail: representative })); }
      }); name.append(requestLink);
      [r.requestCount.toLocaleString(), percent(r.requestCount, total), time(r.averageMillis), time(r.p95Millis), time(r.maximumMillis)].forEach(function (value) { tr.append(node('td', value)); });
      [r.clientErrorCount, r.serverErrorCount].forEach(function (value) { var cell = node('td', value.toLocaleString(), value ? 'api-error' : ''); cell.append(node('small', percent(value, r.knownStatusCount))); tr.append(cell); });
      byId('rows').append(tr);
    });
    var all = slice.length > 0 && slice.every(function (r) { return selected.has(r.endpoint); });
    byId('select-page').checked = all; byId('select-page').disabled = !slice.length;
    byId('select-page').indeterminate = !all && slice.some(function (r) { return selected.has(r.endpoint); });
    byId('prev').disabled = page === 0; byId('next').disabled = (page + 1) * size >= itemCount;
    byId('page-label').parentElement.hidden = mode === 'distribution';
    byId('page-label').textContent = (group ? group + ' / ' : '') + itemCount + (mode === 'groups' ? ' path groups' : ' matching APIs') + ' / page ' + (page + 1) + ' of ' + Math.max(1, Math.ceil(itemCount / size));
    byId('top').disabled = !visible.length;
    renderSelection();
  }
  function resetPages() { page = 0; pages = { distribution: 0, groups: 0, table: 0 }; }
  function resetFilters() {
    byId('search').value = ''; byId('status').value = 'all'; byId('min-count').value = '0'; byId('min-p95').value = '0';
    group = ''; resetPages();
  }
  ['search', 'status', 'min-count', 'min-p95'].forEach(function (id) { byId(id).addEventListener('input', function () { resetPages(); render(); }); });
  byId('depth').addEventListener('change', function () { depth = Number(byId('depth').value); group = ''; resetPages(); render(); });
  byId('group-clear').addEventListener('click', function () { group = ''; resetPages(); render(); });
  byId('reset').addEventListener('click', function () {
    resetFilters(); render();
  });
  document.querySelectorAll('[data-api-view]').forEach(function (b) { b.addEventListener('click', function () { switchMode(b.dataset.apiView); }); });
  document.querySelectorAll('[data-api-sort]').forEach(function (th) { th.querySelector('button').addEventListener('click', function () {
    direction = metric === th.dataset.apiSort && direction === 'desc' ? 'asc' : 'desc'; metric = th.dataset.apiSort; page = 0;
    document.querySelectorAll('[data-api-sort]').forEach(function (header) { header.setAttribute('aria-sort', header === th ? (direction === 'asc' ? 'ascending' : 'descending') : 'none'); }); render();
  }); });
  document.querySelector('[data-api-sort="requestCount"] button').title = 'Retained observations, not lifetime traffic. Frequency alone does not determine business importance.';
  document.querySelector('[data-api-sort="p95Millis"] button').title = '95% of valid retained adapter intervals are at or below this value; small samples are not a benchmark.';
  document.querySelector('[data-api-sort="serverErrorCount"] button').title = 'HTTP 5xx count. Percentage uses known final HTTP statuses, not test assertions.';
  byId('prev').addEventListener('click', function () { page--; render(); });
  byId('next').addEventListener('click', function () { page++; render(); });
  byId('select-page').addEventListener('change', function () { var checked = byId('select-page').checked; visible.slice(page * size, (page + 1) * size).forEach(function (r) { if (checked) { selected.add(r.endpoint); } else { selected.delete(r.endpoint); } }); render(); });
  byId('top').addEventListener('click', function () { visible.slice(0, 20).forEach(function (r) { selected.add(r.endpoint); }); render(); });
  byId('clear').addEventListener('click', function () { selected.clear(); render(); });
  byId('create').addEventListener('click', function () {
    var sources = api.sources(selected, scopedRequests());
    var result = window.ReqoverCaseEditor.createMany(sources.requests);
    byId('export-status').textContent = result.added + ' new drafts / ' + result.reused + ' existing drafts reused / ' + sources.missing.length + ' selected APIs without recent detail.';
    if (sources.requests.length) { window.location.hash = 'test-case-drafts'; }
  });
  byId('export').addEventListener('click', function () {
    if (!selected.size) { return; }
    var sources = api.sources(selected, scopedRequests());
    var bundle = { kind: 'reqover-http-test-draft-bundle', schemaVersion: 1, generatedAt: summary.generatedAt,
      state: 'review-required', replayable: false, sourceDetailLimit: 100,
      scope: scope,
      selectedEndpoints: Array.from(selected), missingSourceEndpoints: sources.missing,
      drafts: sources.requests.map(function (r) { return window.ReqoverTestDrafts.create(r); }) };
    var url = URL.createObjectURL(new Blob([JSON.stringify(bundle, null, 2) + '\n'], { type: 'application/json' }));
    var link = node('a'); link.href = url; link.download = 'reqover-test-draft-bundle.json'; link.click();
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    byId('export-status').textContent = sources.requests.length + ' review-required drafts exported / ' + sources.missing.length + ' selected APIs without recent detail. No tests executed.';
  });
  renderTotals(); render();
})();

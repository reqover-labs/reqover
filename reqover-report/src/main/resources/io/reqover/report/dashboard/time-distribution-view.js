/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
(function () {
  'use strict';
  window.ReqoverTimeView = { init: function (data, choose) {
    var table = document.getElementById('api-heatmap');
    var note = document.getElementById('api-time-count');
    function el(tag, text) { var element = document.createElement(tag); if (text !== undefined) { element.textContent = text; } return element; }
    function label(instant) { return new Date(instant).toLocaleString('en-GB', { timeZone: 'UTC', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' }); }
    var windows = data.windows;
    var slots = window.ReqoverApiWorkspace.timeSlots(windows);
    note.textContent = data.displayedCount + ' displayed / ' + data.totalRetainedCount + ' retained HTTP observations / '
      + data.omittedWindowRequestCount + ' omitted by window limit / ' + data.unsupportedStartCount + ' unsupported start times';
    if (!windows.length) { note.textContent += ' / No available time distribution.'; return function () {}; }
    var max = Math.max.apply(null, windows.flatMap(function (w) { return w.bands.map(function (b) { return b.requestCount; }); }));
    var head = el('thead'), header = el('tr'); header.append(el('th', 'Adapter interval'));
    slots.forEach(function (w) {
      if (w.gapSeconds) { var gap = el('th', 'Gap ' + (w.gapSeconds / 60) + ' min'); gap.className = 'api-heat-gap'; header.append(gap); return; }
      var cell = el('th'), button = el('button', label(w.startedAt)); button.type = 'button';
      button.title = w.startedAt + ' to ' + w.endedAt + ' / all intervals / ' + w.requestCount + ' observations';
      button.addEventListener('click', function () { choose({ startedAt: w.startedAt, endedAt: w.endedAt, band: null }, w.endpoints); });
      cell.append(button); header.append(cell);
    }); head.append(header); table.append(head);
    var body = el('tbody');
    [4, 3, 2, 1, 0, 5].forEach(function (index) {
      var row = el('tr'); var heading = el('th', windows[0].bands[index].label); heading.scope = 'row'; row.append(heading);
      slots.forEach(function (w) {
        if (w.gapSeconds) { var gap = el('td'); gap.className = 'api-heat-gap'; gap.title = 'Compressed gap: no retained window; not proof of no traffic'; row.append(gap); return; }
        var b = w.bands[index], cell = el('td'), button = el('button', b.requestCount ? String(b.requestCount) : ''); button.type = 'button';
        button.className = 'api-heat-cell'; button.dataset.level = b.requestCount ? String(Math.max(1, Math.ceil(4 * Math.log1p(b.requestCount) / Math.log1p(max)))) : '0';
        button.dataset.start = w.startedAt; button.dataset.band = String(index);
        if (b.serverErrorCount) { button.classList.add('has-5xx'); } else if (b.clientErrorCount) { button.classList.add('has-4xx'); }
        button.disabled = b.requestCount === 0;
        button.setAttribute('aria-label', label(w.startedAt) + ' UTC / ' + b.label + ' / ' + b.requestCount + ' observations / ' + b.serverErrorCount + ' HTTP 5xx');
        button.title = w.startedAt + ' / ' + b.label + ' / ' + b.requestCount + ' retained / 4xx: ' + b.clientErrorCount + ' / 5xx: ' + b.serverErrorCount + ' / unknown status: ' + (b.requestCount - b.knownStatusCount);
        button.addEventListener('click', function () { choose({ startedAt: w.startedAt, endedAt: w.endedAt, band: index }, b.endpoints); });
        cell.append(button); row.append(cell);
      }); body.append(row);
    }); table.append(body);
    return function (scope) {
      table.querySelectorAll('.api-heat-cell').forEach(function (button) {
        button.setAttribute('aria-pressed', String(!!scope && button.dataset.start === scope.startedAt && (scope.band === null || Number(button.dataset.band) === scope.band)));
      });
    };
  } };
})();

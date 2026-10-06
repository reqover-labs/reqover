(function () {
  'use strict';
  var data = document.getElementById('reqover-comparison-data');
  if (!data) { return; }
  var api = window.ReqoverRecordingComparison;
  var current = api.read(JSON.parse(data.textContent));
  var baseline = null;
  var result = null;
  var file = document.getElementById('reqover-baseline-file');
  var error = document.getElementById('reqover-comparison-error');
  var confirmed = document.getElementById('reqover-comparison-confirmed');
  var filter = document.getElementById('reqover-comparison-filter');
  var sort = document.getElementById('reqover-comparison-sort');
  var reset = document.getElementById('reqover-comparison-reset');
  var rows = document.getElementById('reqover-comparison-rows');
  var importNumber = 0;
  var notices = {
    'baseline-measurements-unavailable': 'Baseline has no retained HTTP diagnostics.',
    'current-measurements-unavailable': 'Current recording has no retained HTTP diagnostics.',
    'sample-counts-differ': 'Retained or timed sample counts differ between recordings.',
    'small-timing-samples': 'At least one endpoint has fewer than 20 timed samples; p95 is descriptive only.',
    'unknown-statuses': 'Unknown final statuses are excluded from HTTP error percentages.'
  };
  var presence = { both: 'Observed in both', 'baseline-only': 'Not observed now',
    'current-only': 'Not observed before', 'baseline-unavailable': 'Baseline unavailable',
    'current-unavailable': 'Current unavailable' };

  function text(id, value) { document.getElementById(id).textContent = value; }
  function el(tag, className, value) {
    var element = document.createElement(tag);
    if (className) { element.className = className; }
    if (value !== undefined) { element.textContent = value; }
    return element;
  }
  function number(value) {
    if (value === null) { return 'Unknown'; }
    return value !== 0 && Math.abs(value) < 0.001 ? value.toExponential(2)
      : value.toFixed(Math.abs(value) < 1 ? 4 : 2);
  }
  function time(value) { return value === null ? 'Not timed' : number(value) + ' ms'; }
  function percent(value) { return value === null ? 'Unknown' : value.toFixed(1) + '%'; }
  function summaryMeta(value) { return value.httpRequestCount + ' retained HTTP observations / ' + value.generatedAt; }
  function cellDelta(value, before, after, unit, format) {
    var cell = el('td');
    cell.append(el('strong', value === null ? '' : value > 0 ? 'delta-up' : value < 0 ? 'delta-down' : '',
      value === null ? 'Not comparable' : (value > 0 ? '+' : '') + number(value) + unit));
    cell.append(el('small', '', format(before) + ' \u2192 ' + format(after)));
    return cell;
  }
  function renderRows() {
    if (!result) { return; }
    var query = filter.value.trim().toLowerCase();
    var visible = result.rows.filter(function (row) { return !query || row.endpoint.toLowerCase().includes(query); });
    visible.sort(function (a, b) {
      var name = a.endpoint < b.endpoint ? -1 : a.endpoint > b.endpoint ? 1 : 0;
      var field = sort.value === 'errors' ? 'failureDeltaPoints' : 'p95DeltaMillis';
      if (sort.value === 'endpoint') { return name; }
      if (sort.value === 'requests') { return (b.current ? b.current.requestCount : 0) - (a.current ? a.current.requestCount : 0) || name; }
      if (a[field] === null && b[field] !== null) { return 1; }
      if (b[field] === null && a[field] !== null) { return -1; }
      return (b[field] || 0) - (a[field] || 0) || name;
    });
    rows.replaceChildren();
    visible.forEach(function (row) {
      var tr = el('tr'); tr.dataset.endpoint = row.endpoint;
      var title = el('td'); title.append(el('code', '', row.endpoint));
      var counts = el('td');
      counts.append(el('strong', '', (row.baseline ? row.baseline.requestCount : '\u2014') + ' \u2192 ' + (row.current ? row.current.requestCount : '\u2014')));
      counts.append(el('small', '', 'Timed ' + (row.baseline ? row.baseline.timedRequestCount : '\u2014') + ' / ' + (row.current ? row.current.timedRequestCount : '\u2014')));
      tr.append(title, counts,
        cellDelta(row.averageDeltaMillis, row.baseline ? row.baseline.averageMillis : null, row.current ? row.current.averageMillis : null, ' ms', time),
        cellDelta(row.p95DeltaMillis, row.baseline ? row.baseline.p95Millis : null, row.current ? row.current.p95Millis : null, ' ms', time),
        cellDelta(row.failureDeltaPoints, api.failurePercent(row.baseline), api.failurePercent(row.current), ' pp', percent));
      var scope = el('td'); scope.append(el('strong', '', presence[row.presence]));
      if (row.baseline && row.current) {
        scope.append(el('small', '', 'Known status ' + row.baseline.knownStatusCount + ' / ' + row.current.knownStatusCount));
      }
      tr.append(scope); rows.append(tr);
    });
    document.getElementById('reqover-comparison-no-match').hidden = visible.length > 0;
  }
  function renderContext() {
    text('reqover-comparison-context', confirmed.checked
      ? 'Conditions manually checked by user. Still no automatic performance verdict.'
      : 'Conditions not recorded. No automatic performance verdict.');
  }
  function render() {
    result = baseline ? api.between(baseline, current) : null;
    document.getElementById('reqover-comparison-empty').hidden = !!baseline;
    document.getElementById('reqover-comparison-results').hidden = !baseline;
    reset.disabled = !baseline;
    var list = document.getElementById('reqover-comparison-notices'); list.replaceChildren();
    if (result) {
      result.warnings.forEach(function (warning) { if (notices[warning]) { list.append(el('li', '', notices[warning])); } });
      renderRows();
    }
    renderContext();
  }
  file.addEventListener('change', async function () {
    var selected = file.files[0]; if (!selected) { return; }
    var token = ++importNumber;
    try {
      if (selected.size > 10 * 1024 * 1024) { throw new Error('Baseline JSON must be at most 10 MiB.'); }
      var candidate = api.read(JSON.parse(await selected.text()));
      if (token !== importNumber) { return; }
      baseline = candidate; confirmed.checked = false;
      error.hidden = true; error.textContent = '';
      text('reqover-baseline-name', selected.name); text('reqover-baseline-meta', summaryMeta(candidate));
      render();
    } catch (problem) {
      if (token !== importNumber) { return; }
      error.textContent = 'Import failed: ' + problem.message; error.hidden = false;
    } finally { file.value = ''; }
  });
  reset.addEventListener('click', function () {
    importNumber++; baseline = null; result = null; confirmed.checked = false;
    error.hidden = true; error.textContent = ''; rows.replaceChildren();
    text('reqover-baseline-name', 'Not imported'); text('reqover-baseline-meta', ''); render();
  });
  document.getElementById('reqover-comparison-export').addEventListener('click', function () {
    var url = URL.createObjectURL(new Blob([JSON.stringify(current, null, 2) + '\n'], { type: 'application/json' }));
    var link = el('a'); link.href = url; link.download = 'reqover-recorded-summary.json'; link.click();
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
  });
  filter.addEventListener('input', renderRows); sort.addEventListener('change', renderRows);
  confirmed.addEventListener('change', renderContext);
  text('reqover-current-name', current.available ? 'Retained HTTP summary' : 'No retained HTTP diagnostics');
  text('reqover-current-meta', summaryMeta(current)); render();
})();

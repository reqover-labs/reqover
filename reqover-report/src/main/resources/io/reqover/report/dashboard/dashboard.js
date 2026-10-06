(function () {
  'use strict';
  var dataElement = document.getElementById('reqover-map-data');
  var workspace = document.getElementById('reqover-workspace');
  if (!dataElement || !workspace) { return; }
  var data = JSON.parse(dataElement.textContent);
  var requests = data.requests || [];
  var details = Array.from(document.querySelectorAll('details.request-detail'));
  var codes = (data.reverseIndex || []).slice().sort(function (a, b) {
    return b.endpoints.length - a.endpoints.length || key(a).localeCompare(key(b));
  });
  var codeIndex = new Map(codes.map(function (c) { return [key(c), c]; }));
  var shared = codes.filter(function (c) { return c.endpoints.length > 1; });
  var selection = document.getElementById('reqover-map-selection');
  var stage = document.getElementById('reqover-graph-stage');
  var plane = document.getElementById('reqover-graph-plane');
  var size = document.getElementById('reqover-graph-size');
  var lines = document.getElementById('reqover-graph-lines');
  var nodes = document.getElementById('reqover-graph-nodes');
  var inspector = document.getElementById('reqover-inspector');
  var threshold = document.getElementById('reqover-slow-ms');
  var search = document.getElementById('reqover-filter');
  var motion = window.matchMedia('(prefers-reduced-motion: reduce)');
  var animation = document.getElementById('reqover-animation');
  var mode = requests.length ? 'request' : 'retest';
  var selectedRequest = 0;
  var selectedCode = 0;
  var zoom = 1;
  var graphHeight = 330;
  var graphWidth = 860;
  var originalHtml = '<!doctype html>\n' + document.documentElement.outerHTML;

  // These details and JSON share the renderer's identical latest-100 ordering.
  // Use the Java-computed intervals rather than rounding nanoseconds in JS Date.
  requests.forEach(function (r, i) {
    var detail = details[i];
    var text = detail && detail.getAttribute('data-request-duration');
    r.duration = text ? Number(text) : null;
    r.finalStatus = detail ? Number(detail.getAttribute('data-request-status')) : -1;
  });

  function key(c) { return c.className + '#' + c.methodName + c.descriptor; }
  function simple(name) { return name.slice(name.lastIndexOf('.') + 1); }
  function shortCode(c) { return simple(c.className) + '#' + c.methodName; }
  function time(value) { return value === null ? 'Not timed' : value.toFixed(2) + ' ms'; }
  function status(r) { return r.finalStatus === -1 ? 'Unknown status' : 'HTTP ' + r.finalStatus; }
  function slowMs() { var value = threshold ? Number(threshold.value) : 1000; return Number.isFinite(value) && value > 0 ? value : 1000; }
  function risk(r) { return r.finalStatus >= 500 ? 3 : r.finalStatus >= 400 ? 2 : r.duration !== null && r.duration >= slowMs() ? 1 : 0; }
  function riskOrder(a, b) { return risk(b.r) - risk(a.r) || (b.r.duration || 0) - (a.r.duration || 0) || a.i - b.i; }
  function el(tag, className, text) {
    var element = document.createElement(tag);
    if (className) { element.className = className; }
    if (text !== undefined) { element.textContent = text; }
    return element;
  }
  function fields(values) {
    var dl = el('dl', 'inspector-fields');
    values.forEach(function (v) { dl.append(el('dt', '', v[0]), el('dd', '', v[1])); });
    inspector.append(dl);
  }
  function heading(kind, name) {
    inspector.replaceChildren(el('span', 'inspector-label', kind), el('h4', '', name));
  }
  function codeOf(r) {
    var result = new Map();
    r.classes.forEach(function (type) {
      type.methods.forEach(function (method) {
        var code = { className: type.className, methodName: method.methodName, descriptor: method.descriptor };
        result.set(key(code), code);
      });
    });
    return Array.from(result.values()).sort(function (a, b) {
      var ac = codeIndex.get(key(a)); var bc = codeIndex.get(key(b));
      return (bc ? bc.endpoints.length : 0) - (ac ? ac.endpoints.length : 0) || key(a).localeCompare(key(b));
    });
  }
  function inspectRequest(r) {
    heading('RECORDED REQUEST', r.endpoint);
    var draft = el('button', 'case-command draft-action'); draft.type = 'button';
    draft.append(document.getElementById('reqover-draft-icon').content.cloneNode(true), el('span', '', 'Create test draft'));
    draft.addEventListener('click', function () { createDraft(r); });
    inspector.append(draft);
    fields([['Request', r.requestId], ['Status', status(r)], ['Interval', time(r.duration)],
      ['Methods', String(codeOf(r).length)], ['Threads', r.threadNames.join(', ') || 'Not recorded'],
      ['Started', r.startedAt], ['Inputs', 'Not collected'], ['Exceptions', 'Not collected']]);
    inspector.append(el('p', 'inspector-foot', 'HTTP errors and recorded intervals only. No method timing or call order.'));
  }
  function inspectCode(c) {
    var linked = codeIndex.get(key(c));
    var endpoints = linked ? linked.endpoints : [];
    heading(endpoints.length > 1 ? 'SHARED CODE' : 'OBSERVED METHOD', shortCode(c));
    fields([['Class', c.className], ['Signature', c.methodName + c.descriptor], ['Endpoints', String(endpoints.length)]]);
    inspector.append(el('p', 'inspector-subtitle', 'Observed retest candidates'));
    endpoints.forEach(function (endpoint) { inspector.append(endpointLink(endpoint)); });
    inspector.append(el('p', 'inspector-foot', 'Unobserved callers may also depend on this code. This is not proof of complete impact.'));
  }
  function endpointLink(endpoint) {
    var a = el('a', 'inspector-endpoint', endpoint);
    a.href = '#endpoint-code';
    a.addEventListener('click', function () {
      search.value = endpoint;
      search.dispatchEvent(new Event('input'));
      showView('endpoint-code');
    });
    return a;
  }
  function inspectEndpoint(endpoint) {
    heading('RETEST CANDIDATE', endpoint);
    var observed = requests.filter(function (r) { return r.endpoint === endpoint; });
    var union = data.endpoints.find(function (e) { return e.endpoint === endpoint; });
    fields([['Requests', union ? String(union.requestCount) : 'Not recorded'],
      ['Recent 4xx/5xx', String(observed.filter(function (r) { return r.finalStatus >= 400; }).length)]]);
    inspector.append(endpointLink(endpoint));
  }
  function label(text, x, y) {
    var span = el('span', 'graph-lane', text);
    span.style.left = x + 'px'; span.style.top = y + 'px';
    nodes.append(span);
  }
  function node(kind, title, subtitle, x, y, width, onClick, identity) {
    var button = el('button', 'graph-node ' + kind);
    button.type = 'button';
    button.style.left = x + 'px'; button.style.top = y + 'px';
    button.style.width = width + 'px'; button.style.height = '70px';
    button.title = title + ' · ' + subtitle;
    button.setAttribute('aria-label', button.title);
    button.dataset.nodeKind = kind.split(' ')[0];
    if (identity) { button.dataset.codeKey = identity; }
    button.append(el('strong', '', title), el('small', '', subtitle));
    button.addEventListener('click', function () {
      nodes.querySelectorAll('.selected').forEach(function (n) { n.classList.remove('selected'); });
      button.classList.add('selected');
      onClick();
    });
    nodes.append(button);
  }
  function edge(x1, y1, x2, y2, kind, relation) {
    var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', 'M' + x1 + ',' + y1 + ' C' + x1 + ',' + (y1 + 35) + ' ' + x2 + ',' + (y2 - 35) + ' ' + x2 + ',' + y2);
    path.setAttribute('class', 'graph-edge ' + kind);
    path.dataset.relation = relation;
    lines.append(path);
  }
  function scale() {
    plane.style.width = graphWidth + 'px'; plane.style.height = graphHeight + 'px';
    plane.style.transform = 'scale(' + zoom + ')';
    size.style.width = graphWidth * zoom + 'px'; size.style.height = graphHeight * zoom + 'px';
  }
  function fit() { zoom = Math.max(0.65, Math.min(1, (stage.clientWidth - 12) / graphWidth)); scale(); }
  function renderGraph() {
    nodes.replaceChildren(); lines.replaceChildren();
    if (mode === 'request') { requestGraph(requests[selectedRequest]); }
    else { retestGraph(codes[selectedCode]); }
    lines.setAttribute('viewBox', '0 0 ' + graphWidth + ' ' + graphHeight);
    scale();
  }
  function requestGraph(r) {
    if (!r) { emptyGraph('No retained HTTP observations.'); return; }
    var methods = codeOf(r);
    var visible = methods.slice(0, 12);
    graphHeight = Math.max(330, 220 + Math.ceil(visible.length / 3) * 94);
    label('HTTP entry', 24, 24); label('Request scope', 296, 24); label('HTTP result', 616, 24);
    node('entry', r.endpoint, 'Incoming request', 24, 48, 216, function () { inspectRequest(r); });
    node('bucket', r.requestId, methods.length + ' observed methods · ' + r.threadNames.length + ' threads', 296, 48, 242, function () { inspectRequest(r); });
    node('result' + (r.finalStatus >= 400 ? ' failure' : ''), status(r), time(r.duration), 616, 48, 216, function () { inspectRequest(r); });
    edge(240, 83, 296, 83, '', 'request-lifecycle');
    edge(538, 83, 616, 83, r.finalStatus >= 400 ? 'failure' : '', 'request-lifecycle');
    label('Observed method set' + (methods.length > visible.length ? ' · showing ' + visible.length + ' of ' + methods.length : ''), 24, 167);
    visible.forEach(function (c, i) {
      var x = 24 + (i % 3) * 276; var y = 208 + Math.floor(i / 3) * 94;
      var linked = codeIndex.get(key(c)); var count = linked ? linked.endpoints.length : 0;
      edge(417, 118, x + 126, y, 'association' + (count > 1 ? ' shared' : ''), 'request-method-membership');
      node('method' + (count > 1 ? ' shared' : ''), shortCode(c), count > 1 ? count + ' endpoints · shared code' : 'Observed in this request', x, y, 252, function () { inspectCode(c); }, key(c));
    });
    if (!methods.length) { label('No application methods observed', 24, 226); }
    inspectRequest(r);
  }
  function retestGraph(c) {
    if (!c) { emptyGraph('No code-to-endpoint observations.'); return; }
    var endpoints = c.endpoints.slice(0, 16);
    graphHeight = Math.max(330, 210 + Math.ceil(endpoints.length / 3) * 94);
    label('Observed code', 24, 24);
    node('method' + (c.endpoints.length > 1 ? ' shared' : ''), shortCode(c), c.endpoints.length + ' observed endpoint candidates', 272, 52, 304, function () { inspectCode(c); }, key(c));
    label('Retest candidates' + (c.endpoints.length > endpoints.length ? ' · showing ' + endpoints.length + ' of ' + c.endpoints.length : ''), 24, 162);
    endpoints.forEach(function (endpoint, i) {
      var x = 24 + (i % 3) * 276; var y = 204 + Math.floor(i / 3) * 94;
      edge(424, 122, x + 126, y, 'association shared', 'code-endpoint-observation');
      node('endpoint', endpoint, 'Observed dependency · retest candidate', x, y, 252, function () { inspectEndpoint(endpoint); });
    });
    inspectCode(c);
  }
  function emptyGraph(message) {
    graphHeight = 330;
    nodes.append(el('p', 'graph-empty', message));
    heading('NO OBSERVATIONS', message);
  }
  function choose(newMode, index) {
    mode = newMode;
    if (mode === 'request') { selectedRequest = index; } else { selectedCode = index; }
    populateSelection(); renderGraph();
  }
  function populateSelection() {
    selection.replaceChildren();
    var values = mode === 'request' ? requests : codes;
    values.forEach(function (value, i) {
      var option = el('option', '', mode === 'request'
        ? status(value) + ' · ' + time(value.duration) + ' · ' + value.endpoint + ' · ' + value.requestId
        : value.endpoints.length + ' endpoints · ' + shortCode(value));
      option.value = String(i); selection.append(option);
    });
    if (!values.length) { selection.append(el('option', '', 'No observations')); selection.disabled = true; }
    else { selection.disabled = false; selection.value = String(mode === 'request' ? selectedRequest : selectedCode); }
    document.querySelectorAll('[data-map-mode]').forEach(function (button) {
      button.setAttribute('aria-pressed', String(button.dataset.mapMode === mode));
      button.disabled = button.dataset.mapMode === 'request' && !requests.length;
    });
  }
  function queue() {
    var list = document.getElementById('reqover-attention');
    var query = search.value.trim().toLowerCase();
    var issues = requests.map(function (r, i) { return { r: r, i: i }; }).filter(function (item) {
      return risk(item.r) > 0 && (!query || (item.r.requestId + ' ' + item.r.endpoint + ' ' + codeOf(item.r).map(key).join(' ')).toLowerCase().includes(query));
    }).sort(riskOrder);
    var sharedMatches = shared.filter(function (c) { return !query || (key(c) + ' ' + c.endpoints.join(' ')).toLowerCase().includes(query); });
    list.replaceChildren();
    issues.slice(0, 4).forEach(function (item) {
      var r = item.r; var failed = r.finalStatus >= 400;
      var button = el('button', 'attention-item' + (failed ? ' failure' : ''));
      button.type = 'button';
      var text = el('span'); text.append(el('strong', '', r.endpoint), el('small', '', r.requestId + ' · ' + time(r.duration)));
      button.append(text, el('b', 'attention-badge', failed ? String(r.finalStatus) : 'SLOW'));
      button.addEventListener('click', function () { choose('request', item.i); });
      list.append(button);
    });
    sharedMatches.slice(0, 2).forEach(function (c) {
      var button = el('button', 'attention-item'); button.type = 'button';
      var text = el('span'); text.append(el('strong', '', shortCode(c)), el('small', '', 'Shared across observed endpoints'));
      button.append(text, el('b', 'attention-badge', c.endpoints.length + ' endpoints'));
      button.addEventListener('click', function () { choose('retest', codes.indexOf(c)); });
      list.append(button);
    });
    if (!list.children.length) { list.append(el('p', 'graph-legend', 'No matching failure, delay, or shared-code observations.')); }
    document.getElementById('reqover-queue-count').textContent = issues.length + ' flagged in latest ' + requests.length + ' · ' + sharedMatches.length + ' shared methods';
  }
  function showView(id) {
    var allowed = ['request-overview', 'request-list', 'endpoint-code', 'code-endpoint', 'ci-report', 'test-case-drafts'];
    if (!allowed.includes(id) || !document.getElementById(id)) { id = 'request-overview'; }
    document.querySelectorAll('main > section.section').forEach(function (section) { section.hidden = section.id !== id; });
    document.querySelectorAll('.dashboard-link').forEach(function (a) {
      if (a.hash === '#' + id) { a.setAttribute('aria-current', 'page'); } else { a.removeAttribute('aria-current'); }
    });
    document.getElementById('reqover-view-title').textContent = {
      'request-overview': 'Validation overview', 'request-list': 'Observed requests',
      'endpoint-code': 'API to code', 'code-endpoint': 'Retest candidates', 'ci-report': 'CI artifacts', 'test-case-drafts': 'Test drafts'
    }[id];
    document.getElementById('reqover-filter-box').hidden = id === 'ci-report' || id === 'test-case-drafts';
    if (id === 'request-overview') { requestAnimationFrame(fit); }
  }
  function setAnimation(playing) {
    playing = playing && !motion.matches;
    stage.classList.toggle('graph-running', playing);
    stage.classList.toggle('graph-paused', !playing);
    animation.setAttribute('aria-label', motion.matches ? 'Animation disabled (reduced motion)' : playing ? 'Pause animation' : 'Play animation');
    animation.title = animation.getAttribute('aria-label');
    animation.disabled = motion.matches;
    animation.replaceChildren(document.getElementById(playing ? 'reqover-pause-icon' : 'reqover-play-icon').content.cloneNode(true));
  }

  function createDraft(request) {
    window.ReqoverCaseEditor.create(request);
    window.location.hash = 'test-case-drafts';
    showView('test-case-drafts');
  }

  details.forEach(function (detail, index) {
    var action = el('button', 'case-command'); action.type = 'button';
    action.append(document.getElementById('reqover-draft-icon').content.cloneNode(true), el('span', '', 'Create test draft'));
    action.addEventListener('click', function () { createDraft(requests[index]); });
    detail.querySelector('.request-code').append(action);
  });

  document.querySelectorAll('[data-map-mode]').forEach(function (button) {
    button.addEventListener('click', function () { choose(button.dataset.mapMode, button.dataset.mapMode === 'request' ? selectedRequest : selectedCode); });
  });
  selection.addEventListener('change', function () { choose(mode, Number(selection.value)); });
  animation.addEventListener('click', function () { setAnimation(!stage.classList.contains('graph-running')); });
  motion.addEventListener('change', function () { setAnimation(!motion.matches); });
  document.getElementById('reqover-zoom-in').addEventListener('click', function () { zoom = Math.min(1.5, zoom + 0.15); scale(); });
  document.getElementById('reqover-zoom-out').addEventListener('click', function () { zoom = Math.max(0.35, zoom - 0.15); scale(); });
  document.getElementById('reqover-fit').addEventListener('click', fit);
  if (threshold) { threshold.addEventListener('input', queue); }
  search.addEventListener('input', queue);
  window.addEventListener('hashchange', function () { showView(window.location.hash.slice(1)); });
  window.addEventListener('resize', fit);
  document.getElementById('reqover-download').addEventListener('click', function () {
    var url = URL.createObjectURL(new Blob([originalHtml], { type: 'text/html;charset=utf-8' }));
    var link = el('a'); link.href = url; link.download = 'reqover-report.html'; link.click();
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
  });
  if (requests.length) {
    selectedRequest = requests.map(function (r, i) { return { r: r, i: i }; }).sort(riskOrder)[0].i;
  }
  populateSelection(); renderGraph(); queue();
  workspace.hidden = false;
  document.body.classList.add('dashboard-enhanced');
  showView(window.location.hash.slice(1));
  fit(); setAnimation(!motion.matches);
})();

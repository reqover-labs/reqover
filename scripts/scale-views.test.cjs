// SPDX-License-Identifier: Apache-2.0
// Controller-only tests with DOM doubles. No browser or local report is opened.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const root = '../reqover-report/src/main/resources/io/reqover/report/dashboard/';
const api = require(root + 'api-workspace.js');
class Element {
  constructor(tag = 'div') { this.tag = tag; this.children = []; this.dataset = {}; this.style = {}; this.attributes = {}; this.handlers = {}; this.value = ''; this.hidden = false; this.className = ''; this.ownText = ''; this.classList = { add: c => { this.className += ' ' + c; } }; }
  append(...children) { children.forEach(child => { this.children.push(child); child.parentElement = this; }); }
  replaceChildren(...children) { this.children = []; this.append(...children); }
  set textContent(value) { this.ownText = String(value); this.children = []; }
  get textContent() { return this.ownText + this.children.map(c => c.textContent).join(' '); }
  setAttribute(k, v) { this.attributes[k] = String(v); }
  getAttribute(k) { return this.attributes[k] || null; }
  addEventListener(type, handler) { this.handlers[type] = handler; }
  fire(type) { this.handlers[type]?.({ type }); }
  focus() {}
  querySelectorAll(selector) { return this.children.flatMap(c => [(selector === c.tag || (selector.startsWith('.') && c.className.split(' ').includes(selector.slice(1)))) ? c : null, ...c.querySelectorAll(selector)]).filter(Boolean); }
  querySelector(selector) { return this.querySelectorAll(selector)[0]; }
}
const row = n => ({ endpoint: 'GET /orders', requestCount: n, knownStatusCount: n, clientErrorCount: 0, serverErrorCount: 0, timedRequestCount: n, averageMillis: 1000, p95Millis: 1000, maximumMillis: 1000, cumulativeMillis: n * 1000 });
test('distribution -> groups -> API -> request respects selected time and missing details', () => {
  const nodes = new Map();
  const get = id => { if (!nodes.has(id)) { nodes.set(id, new Element()); } return nodes.get(id); };
  const views = ['distribution', 'groups', 'table'].map(mode => { const b = new Element('button'); b.dataset.apiView = mode; return b; });
  const headers = ['endpoint','requestCount','averageMillis','p95Millis','maximumMillis','clientErrorCount','serverErrorCount'].map(metric => { const th = new Element('th'); th.dataset.apiSort = metric; th.append(new Element('button')); return th; });
  const windowData = (start, count, band) => ({ startedAt: start, endedAt: new Date(Date.parse(start) + 300000).toISOString(), requestCount: count, endpoints: [row(count)],
    bands: Array.from({ length: 6 }, (_, i) => ({ band: i, label: 'band-' + i, requestCount: i === band ? count : 0, knownStatusCount: i === band ? count : 0, clientErrorCount: 0, serverErrorCount: 0, endpoints: i === band ? [row(count)] : [] })) });
  const windows = [windowData('2026-10-12T10:00:00Z', 150, 3), windowData('2026-10-12T10:05:00Z', 1, 0)];
  get('reqover-comparison-data').textContent = JSON.stringify({ httpRequestCount: 151, endpoints: [row(151)] });
  get('reqover-time-data').textContent = JSON.stringify({ windows, totalRetainedCount: 151, displayedCount: 151, omittedWindowRequestCount: 0, unsupportedStartCount: 0 });
  get('reqover-map-data').textContent = JSON.stringify({ requests: [{ unitType: 'http-request', endpoint: 'GET /orders', requestId: 'latest', startedAt: windows[1].startedAt, endedAt: windows[1].endedAt, statusCode: 200, classes: [] }] });
  const detail = new Element(); detail.dataset.requestDuration = '20';
  const events = [];
  const window = { ReqoverApiWorkspace: api, location: {}, dispatchEvent: event => events.push(event) };
  get('api-page-label').parentElement = new Element(); get('api-status').value = 'all';
  const document = { getElementById: get, createElement: tag => new Element(tag), querySelectorAll: selector => selector === '[data-api-view]' ? views : selector === '[data-api-sort]' ? headers : selector === 'details.request-detail' ? [detail] : [],
    querySelector(selector) { if (selector.includes('data-api-sort')) { return headers.find(h => selector.includes('"' + h.dataset.apiSort + '"')).children[0]; } return get('api-context'); } };
  const context = { window, document, CustomEvent: class { constructor(type, options) { this.type = type; this.detail = options.detail; } }, setTimeout() {} };
  for (const name of ['time-distribution-view.js', 'api-workspace-view.js']) {
    vm.runInNewContext(fs.readFileSync('reqover-report/src/main/resources/io/reqover/report/dashboard/' + name, 'utf8'), context);
  }
  views[0].fire('click'); assert.equal(get('api-distribution').hidden, false);
  const cells = get('api-heatmap').querySelectorAll('.api-heat-cell');
  cells.find(c => c.dataset.start === windows[0].startedAt && c.dataset.band === '3').fire('click');
  assert.equal(get('api-groups').hidden, false); assert(get('api-totals').textContent.includes('150'));
  views[2].fire('click');
  let check = get('api-rows').querySelector('input'); check.checked = true; check.fire('change');
  assert.equal(get('api-create').disabled, true);
  assert(get('api-source-count').textContent.includes('0 available draft sources / 1 without'));
  get('api-time-clear').fire('click'); assert(get('api-totals').textContent.includes('151'));
  views[0].fire('click');
  cells.find(c => c.dataset.start === windows[1].startedAt && c.dataset.band === '0').fire('click');
  views[2].fire('click');
  const link = get('api-rows').querySelectorAll('.api-request-link')[0];
  assert.equal(link.disabled, false); link.fire('click');
  assert.equal(events[0].type, 'reqover-open-request'); assert.equal(events[0].detail.requestId, 'latest');
});

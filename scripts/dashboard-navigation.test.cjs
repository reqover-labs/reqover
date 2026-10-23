// SPDX-License-Identifier: Apache-2.0
// Isolated controller tests with minimal DOM doubles, not browser/page automation.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('reqover-report/src/main/java/io/reqover/report/HtmlCoverageReportRenderer.java', 'utf8');
const script = source.match(/private static final String FILTER_SCRIPT = """([\s\S]*?)""";/)[1].replace('<script>', '').replace('</script>', '');

test('map inspector endpoint links use exact API selection', () => {
  const dashboard = fs.readFileSync('reqover-report/src/main/resources/io/reqover/report/dashboard/dashboard.js', 'utf8');
  const linkFunction = dashboard.slice(dashboard.indexOf('  function endpointLink('), dashboard.indexOf('  function inspectEndpoint('));
  let handler, selected, view;
  const context = { el: () => ({ addEventListener: (_, listener) => { handler = listener; } }),
    window: { ReqoverFilter: { selectEndpoint: endpoint => { selected = endpoint; } } },
    showView: value => { view = value; }, search: { dispatchEvent() {} }, Event: class {} };
  vm.runInNewContext(linkFunction + '; endpointLink("GET /orders/1");', context);
  handler();
  assert.equal(selected, 'GET /orders/1');
  assert.equal(view, 'endpoint-code');
});
test('API navigation is exact while manual search remains a substring filter', () => {
  const listeners = {};
  const input = { value: '', addEventListener: (name, handler) => { listeners[name] = handler; } };
  const endpoints = ['GET /orders/1', 'GET /orders/10', 'GET /Orders/1'].map(name => ({ hidden: false,
    getAttribute: key => key === 'data-endpoint' ? name : name.toLowerCase() }));
  const elements = { 'reqover-filter-box': {}, 'reqover-filter': input, 'reqover-filter-count': {} };
  const window = {};
  vm.runInNewContext(script, { window, document: {
    getElementById: id => elements[id] || null,
    querySelectorAll: selector => selector === 'article.endpoint' ? endpoints : [], addEventListener() {}
  } });
  window.ReqoverFilter.selectEndpoint('GET /orders/1');
  assert.deepEqual(endpoints.map(e => e.hidden), [false, true, true]);
  input.value = 'orders'; listeners.input();
  assert.deepEqual(endpoints.map(e => e.hidden), [false, false, false]);
  window.ReqoverFilter.selectEndpoint('GET /orders/1');
  listeners.keydown({ key: 'Escape' });
  assert.deepEqual(endpoints.map(e => e.hidden), [false, false, false]);
});
test('search shortcut targets the visible workspace and ignores hidden search controls', () => {
  let keydown, focused;
  const elements = {
    'reqover-filter-box': { hidden: false }, 'reqover-filter-count': {},
    'reqover-filter': { value: '', addEventListener() {}, focus() { focused = 'report'; } },
    'api-workspace': { hidden: false }, 'api-search': { focus() { focused = 'workspace'; } }
  };
  const document = { activeElement: null, getElementById: id => elements[id] || null,
    querySelectorAll: () => [], addEventListener: (_, handler) => { keydown = handler; } };
  vm.runInNewContext(script, { window: {}, document });
  keydown({ key: '/', preventDefault() {} }); assert.equal(focused, 'workspace');
  elements['api-workspace'].hidden = true; keydown({ key: '/', preventDefault() {} }); assert.equal(focused, 'report');
  focused = null; elements['reqover-filter-box'].hidden = true;
  keydown({ key: '/', preventDefault() {} }); assert.equal(focused, null);
  elements['api-workspace'].hidden = false; elements['api-toolbar'] = { hidden: true };
  keydown({ key: '/', preventDefault() {} }); assert.equal(focused, null);
});

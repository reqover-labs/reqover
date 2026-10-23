// SPDX-License-Identifier: Apache-2.0
// Isolated controller checks; no browser/page automation.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { Element } = require('./dom-double.cjs');
const api = require('../reqover-report/src/main/resources/io/reqover/report/dashboard/recording-history.js');

test('recording choices survive import/removal and empty states describe the next selection', async () => {
  const nodes = new Map();
  const get = id => { if (!nodes.has(id)) nodes.set(id, new Element()); return nodes.get(id); };
  const document = { getElementById: get, createElement: tag => new Element(tag) };
  vm.runInNewContext(fs.readFileSync('reqover-report/src/main/resources/io/reqover/report/dashboard/recording-history-view.js', 'utf8'),
    { window: { ReqoverRecordingHistory: api }, document, Blob, URL, setTimeout() {} });
  assert.match(get('recording-rows').textContent, /No recordings loaded/);
  const entry = number => ({ kind: 'reqover-recording-entry', schemaVersion: 1,
    id: '00000000-0000-0000-0000-' + String(number).padStart(12, '0'),
    name: 'Recording ' + number, environment: 'local', revision: 'rev', savedAt: '2026-10-12T00:00:00Z', omittedRequestDetails: 0,
    summary: { kind: 'reqover-recorded-summary', schemaVersion: 1, generatedAt: '2026-10-12T00:00:00Z', available: false, httpRequestCount: 0, endpoints: [] } });
  const load = async value => { get('recording-import').files = [{ size: 1000, text: async () => JSON.stringify(value) }]; await get('recording-import').fire('change'); };
  await load({ kind: 'reqover-recording-history', schemaVersion: 1, recordings: [entry(1), entry(2)] });
  assert.equal(get('recording-current').value, entry(1).id);
  assert.equal(get('recording-baseline').value, entry(2).id);
  get('recording-baseline').value = ''; get('recording-baseline').fire('change');
  await load(entry(3));
  assert.equal(get('recording-baseline').value, '', 'A later import must not restore a cleared baseline');
  get('recording-baseline').value = entry(1).id; get('recording-baseline').fire('change');
  assert.match(get('recording-warnings').textContent, /different recording/);
  assert.equal(get('recording-comparison').children.length, 0);
  get('recording-rows').children[0].querySelector('button').fire('click');
  assert.equal(get('recording-current').value, '', 'Removing selected recording clears the selection');
  assert.match(get('recording-warnings').textContent, /Select a current/);
});

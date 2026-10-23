// SPDX-License-Identifier: Apache-2.0
const test = require('node:test');
const assert = require('node:assert/strict');
const scenarios = require('../reqover-report/src/main/resources/io/reqover/report/dashboard/scenarios.js');
const draftApi = require('../reqover-report/src/main/resources/io/reqover/report/dashboard/test-case-drafts.js');
const draft = () => draftApi.update(draftApi.create({ unitType:'http-request', endpoint:'GET /orders/{id}', requestId:'r1', startedAt:'2026-10-12T00:00:00Z', classes:[] }), { path:'/orders/42', expectedStatus:200, maximumClientMillis:500, reviewed:true });
test('scenario roundtrip stores only manual conditions and imports review-required cases', () => {
  const saved = scenarios.fromDrafts([draft()], { name:'Regression', environment:'local', revision:'abc' });
  const loaded = scenarios.read(JSON.parse(JSON.stringify(saved)));
  assert.equal(loaded.cases[0].path, '/orders/42');
  assert.equal(scenarios.toDrafts(loaded)[0].reviewed, false);
  assert.equal(loaded.cases[0].expectedStatus, 200);
  assert(!JSON.stringify(saved).includes('headers'));
});
test('exports preserve explicit assertions and do not contain automatic auth or target', () => {
  const saved = scenarios.fromDrafts([draft()], { name:'Regression', environment:'local', revision:'' });
  const collection = scenarios.postman(saved);
  assert.equal(collection.item[0].request.method, 'GET');
  assert.equal(collection.variable[0].value, '');
  assert(collection.item[0].event[0].script.exec.join('\n').includes('200'));
  const xml = scenarios.jmeter(saved);
  assert(xml.includes('enabled="false"'));
  assert(xml.includes('ResponseAssertion') && xml.includes('DurationAssertion'));
  assert(!xml.includes('Authorization'));
});
test('unreviewed and unsafe requests cannot become executable external plans', () => {
  const saved = scenarios.fromDrafts([draft()], { name:'Regression', environment:'local', revision:'' });
  saved.cases[0].reviewed = false;
  assert.throws(() => scenarios.postman(saved));
  saved.cases[0].path = '//outside.example'; assert.throws(() => scenarios.read(saved));
  saved.cases[0].path = '/ok'; saved.headers = { Authorization:'not-real' }; assert.throws(() => scenarios.read(saved));
});
test('scenario dates use an explicit UTC instant and case count is bounded',()=>{
  const saved=scenarios.fromDrafts([draft()],{name:'Regression',environment:'local',revision:''});
  saved.savedAt='2026';assert.throws(()=>scenarios.read(saved));
  saved.savedAt='2026-10-12T00:00:00Z';saved.cases=Array.from({length:101},(_,i)=>({...saved.cases[0],id:'case-'+i}));assert.throws(()=>scenarios.read(saved));
});
test('JMeter labels cannot introduce user-supplied function expressions',()=>{
  const saved=scenarios.fromDrafts([draft()],{name:'${__groovy(untrusted)}',environment:'local',revision:''});
  saved.cases[0].id='${__P(secret)}';
  const xml=scenarios.jmeter(saved);assert(!xml.includes('${__groovy'));assert(!xml.includes('${__P(secret)'));
});

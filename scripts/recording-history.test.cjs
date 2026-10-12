// SPDX-License-Identifier: Apache-2.0
const test=require('node:test'),assert=require('node:assert/strict');
const api=require('../reqover-report/src/main/resources/io/reqover/report/dashboard/recording-history.js');
const entry=()=>({kind:'reqover-recording-entry',schemaVersion:1,id:'00000000-0000-0000-0000-000000000001',name:'QA',environment:'local',revision:'rev1',savedAt:'2026-10-12T00:00:00Z',omittedRequestDetails:10,
  summary:{kind:'reqover-recorded-summary',schemaVersion:1,generatedAt:'2026-10-12T00:00:00Z',available:false,httpRequestCount:0,endpoints:[]}});
test('recording history is distinct from execution history and drops opaque report data',()=>{
  const value=entry();value.reportJson='not retained by UI';const loaded=api.read(value)[0];assert.equal(loaded.name,'QA');assert.equal(loaded.omittedRequestDetails,10);assert(!Object.hasOwn(loaded,'reportJson'));
  assert(api.compare(loaded,loaded).warnings.some(w=>w.includes('omitted')));
});

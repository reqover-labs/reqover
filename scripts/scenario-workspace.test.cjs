// SPDX-License-Identifier: Apache-2.0
const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const {Element}=require('./dom-double.cjs');
const root='../reqover-report/src/main/resources/io/reqover/report/dashboard/';
const api=require(root+'test-case-drafts.js'),scenarios=require(root+'scenarios.js'),results=require(root+'scenario-results.js');
test('scenario workspace saves, imports review-required cases and preserves state after invalid imports',async()=>{
  let drafts=[api.update(api.create({unitType:'http-request',endpoint:'GET /ok',requestId:'r1',startedAt:'2026-10-12T00:00:00Z',classes:[]}),{path:'/ok',expectedStatus:200,maximumClientMillis:1000,reviewed:true})];
  const nodes=new Map(),anchors=[],blobs=[];
  const get=id=>{if(!nodes.has(id))nodes.set(id,new Element());return nodes.get(id);};
  get('scenario-name').value='Regression';get('scenario-environment').value='local';
  const window={ReqoverScenarios:scenarios,ReqoverScenarioResults:results,ReqoverTestDrafts:api,location:{},addEventListener(){},
    ReqoverCaseEditor:{drafts:()=>structuredClone(drafts),importDrafts:incoming=>{const result=api.addDrafts(drafts,incoming);drafts=result.drafts;return result;}}};
  const tabs=['cases','runs','recordings'].map(view=>{const e=new Element('button');e.dataset.scenarioView=view;return e;});
  const document={getElementById:get,querySelectorAll:()=>tabs,createElement:tag=>{const e=new Element(tag);if(tag==='a')anchors.push(e);return e;}};
  const context={window,document,Blob,URL:{createObjectURL:blob=>{blobs.push(blob);return 'blob:test';},revokeObjectURL(){}},setTimeout(){}};
  vm.runInNewContext(fs.readFileSync('reqover-report/src/main/resources/io/reqover/report/dashboard/scenario-workspace-view.js','utf8'),context);
  assert.equal(get('scenario-postman').disabled,false);get('scenario-save').fire('click');
  const saved=JSON.parse(await blobs[0].text());assert.equal(saved.kind,'reqover-test-scenario');assert.equal(saved.cases[0].path,'/ok');
  get('scenario-import').files=[{size:1000,text:async()=>JSON.stringify(saved)}];await get('scenario-import').fire('change');
  assert.equal(drafts.length,2);assert.equal(drafts[1].reviewed,false);assert.equal(get('scenario-postman').disabled,true);
  get('scenario-import').files=[{size:100,text:async()=>'{}'}];await get('scenario-import').fire('change');
  assert.equal(drafts.length,2);assert(get('scenario-message').textContent.includes('Import failed'));
  assert.equal(anchors[0].download,'reqover-scenario.json');
  tabs[1].fire('click');assert.equal(get('scenario-runs-panel').hidden,false);assert.equal(get('scenario-cases-panel').hidden,true);
  const recorded={kind:'reqover-scenario-run',schemaVersion:1,runId:'00000000-0000-0000-0000-000000000001',fingerprint:'a'.repeat(64),origin:'http://127.0.0.1:8080',startedAt:'2026-10-12T00:00:00Z',endedAt:'2026-10-12T00:00:01Z',plannedAttempts:1,completion:'COMPLETE',scenario:saved,attempts:[{caseId:'case-1',actualStatus:200,elapsedMillis:12,outcome:'PASS'}]};
  get('scenario-runs-import').files=[{size:1000,text:async()=>JSON.stringify(recorded)}];await get('scenario-runs-import').fire('change');
  assert(get('scenario-history-count').textContent.includes('1 loaded'));
  assert(/Passed\s*1/.test(get('scenario-run-summary').textContent));
  get('scenario-runs-import').files=[{size:10,text:async()=>'{}'}];await get('scenario-runs-import').fire('change');
  assert(get('scenario-history-count').textContent.includes('1 loaded'));assert(get('scenario-message').textContent.includes('existing history preserved'));
  get('scenario-history-save').fire('click');assert.equal(anchors.at(-1).download,'reqover-history.json');
});

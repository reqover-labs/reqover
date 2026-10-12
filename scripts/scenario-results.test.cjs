// SPDX-License-Identifier: Apache-2.0
const test = require('node:test');
const assert = require('node:assert/strict');
const model = require('../reqover-report/src/main/resources/io/reqover/report/dashboard/scenario-results.js');
const run = () => ({ kind:'reqover-scenario-run', schemaVersion:1, runId:'00000000-0000-0000-0000-000000000001', fingerprint:'a'.repeat(64), origin:'http://127.0.0.1:8080', startedAt:'2026-10-12T00:00:00Z', endedAt:'2026-10-12T00:00:01Z', plannedAttempts:2, completion:'COMPLETE',
  scenario:{ kind:'reqover-test-scenario', schemaVersion:1, name:'Demo', environment:'local', revision:'rev1', savedAt:'2026-10-12T00:00:00Z', cases:[{ id:'case-1',method:'GET',path:'/ok',expectedStatus:200,maximumClientMillis:1000,reviewed:true }] },
  attempts:[{caseId:'case-1',actualStatus:200,elapsedMillis:10,outcome:'PASS'},{caseId:'case-1',actualStatus:500,elapsedMillis:20,outcome:'STATUS_MISMATCH'}] });
test('real assertion outcomes are separate from HTTP status counters', () => {
  const summary=model.summary(model.readRun(run()));
  assert.equal(summary.passed,1); assert.equal(summary.failed,1); assert.equal(summary.serverErrors,1);
  assert.equal(summary.averageMillis,15); assert.equal(summary.p95Millis,20);
});
test('comparison warns about different conditions and incomplete execution', () => {
  const a=run(),b=run(); b.origin='http://127.0.0.1:8081';
  assert.equal(model.compare(a,b).comparable,false);
  b.origin=a.origin; b.completion='BUDGET_EXHAUSTED';b.attempts=[];
  const result=model.compare(a,b);assert(result.warnings.length);assert.equal(result.current.averageMillis,null);
});
test('history bounds, secret fields and inconsistent passing results are rejected', () => {
  const a=run();a.attempts[1].outcome='PASS';assert.throws(()=>model.readRun(a));
  const b=run();b.headers={token:'not-real'};assert.throws(()=>model.readRun(b));
  assert.throws(()=>model.readHistory({kind:'reqover-run-history',schemaVersion:1,runs:Array.from({length:21},run)}));
});

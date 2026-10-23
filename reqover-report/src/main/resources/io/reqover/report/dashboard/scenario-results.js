/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
(function (root,factory) {
  if(typeof module==='object' && module.exports){module.exports=factory(require('./scenarios.js'));}
  else {root.ReqoverScenarioResults=factory(root.ReqoverScenarios);}
})(typeof globalThis!=='undefined'?globalThis:this,function(scenarios){
  'use strict';
  function keys(object,allowed){if(!object || typeof object!=='object' || Array.isArray(object) || Object.keys(object).some(function(k){return !allowed.includes(k);})){throw new Error('Unsupported result fields.');}}
  function readRun(value){
    keys(value,['kind','schemaVersion','runId','scenario','fingerprint','origin','startedAt','endedAt','plannedAttempts','completion','attempts']);
    if(value.kind!=='reqover-scenario-run' || value.schemaVersion!==1){throw new Error('Expected scenario run schema 1.');}
    if(!/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i.test(value.runId) || !/^[a-f0-9]{64}$/i.test(value.fingerprint)){throw new Error('Invalid run identity.');}
    if(typeof value.origin!=='string' || value.origin.length>200 || !/^https?:\/\/[^/?#@]+$/.test(value.origin)){throw new Error('Credential-free origin required.');}
    const url=new URL(value.origin);if(url.username || url.password){throw new Error('Credentials prohibited.');}
    if(scenarios.instant(value.endedAt)<scenarios.instant(value.startedAt)){throw new Error('Invalid run interval.');}
    const scenario=scenarios.runnable(value.scenario);
    if(!Number.isInteger(value.plannedAttempts) || value.plannedAttempts<1 || value.plannedAttempts>1000 || !Array.isArray(value.attempts) || value.attempts.length>value.plannedAttempts || !['COMPLETE','BUDGET_EXHAUSTED','INTERRUPTED'].includes(value.completion)){throw new Error('Invalid run bounds.');}
    if(value.completion==='COMPLETE' && value.attempts.length!==value.plannedAttempts){throw new Error('Incomplete attempts marked complete.');}
    const cases=new Map(scenario.cases.map(function(c){return[c.id,c];}));
    const attempts=value.attempts.map(function(a){
      keys(a,['caseId','actualStatus','elapsedMillis','outcome']);const c=cases.get(a.caseId);
      if(!c || !Number.isFinite(a.elapsedMillis) || a.elapsedMillis<0 || a.elapsedMillis>86400000 || !['PASS','STATUS_MISMATCH','CLIENT_LIMIT_EXCEEDED','NETWORK_ERROR','TIMEOUT','INTERRUPTED'].includes(a.outcome)){throw new Error('Invalid attempt.');}
      if(a.actualStatus!==null && (!Number.isInteger(a.actualStatus) || a.actualStatus<100 || a.actualStatus>599)){throw new Error('Invalid actual status.');}
      if(a.outcome==='PASS' && (a.actualStatus!==c.expectedStatus || c.maximumClientMillis!==null && a.elapsedMillis>c.maximumClientMillis)){throw new Error('Inconsistent passing result.');}
      return{caseId:a.caseId,actualStatus:a.actualStatus,elapsedMillis:a.elapsedMillis,outcome:a.outcome};
    });
    return{kind:value.kind,schemaVersion:1,runId:value.runId,scenario:scenario,fingerprint:value.fingerprint,origin:value.origin,
      startedAt:value.startedAt,endedAt:value.endedAt,plannedAttempts:value.plannedAttempts,completion:value.completion,attempts:attempts};
  }
  function summary(run){
    const times=run.attempts.map(function(a){return a.elapsedMillis;}).sort(function(a,b){return a-b;});
    const passed=run.attempts.filter(function(a){return a.outcome==='PASS';}).length;
    return{attempts:times.length,passed:passed,failed:times.length-passed,notRun:run.plannedAttempts-times.length,
      clientErrors:run.attempts.filter(function(a){return a.actualStatus>=400 && a.actualStatus<500;}).length,
      serverErrors:run.attempts.filter(function(a){return a.actualStatus>=500;}).length,
      averageMillis:times.length?times.reduce(function(a,b){return a+b;},0)/times.length:null,
      p95Millis:times.length?times[Math.ceil(times.length*.95)-1]:null,maximumMillis:times.length?times[times.length-1]:null};
  }
  function readHistory(value){
    if(value.kind==='reqover-scenario-run'){return[readRun(value)];}
    keys(value,['kind','schemaVersion','omittedRuns','runs']);
    if(value.kind!=='reqover-run-history' || value.schemaVersion!==1 || !Array.isArray(value.runs) || value.runs.length>20){throw new Error('History requires schema 1 and at most 20 loaded runs.');}
    if(value.omittedRuns!==undefined && (!Number.isSafeInteger(value.omittedRuns) || value.omittedRuns<0)){throw new Error('Invalid omitted history count.');}
    return value.runs.map(readRun);
  }
  function compare(before,after){
    before=readRun(before);after=readRun(after);
    const warnings=['Imported records are not authenticated execution evidence.','No automatic performance verdict; verify test conditions.'];
    const comparable=before.fingerprint===after.fingerprint && before.origin===after.origin && before.scenario.environment===after.scenario.environment && JSON.stringify(before.scenario.cases)===JSON.stringify(after.scenario.cases);
    if(!comparable)warnings.push('Scenario conditions, origin or environment differ.');
    if(before.plannedAttempts!==after.plannedAttempts)warnings.push('Planned attempt counts differ.');
    if(before.completion!=='COMPLETE' || after.completion!=='COMPLETE')warnings.push('At least one run was incomplete.');
    if(before.attempts.length<20 || after.attempts.length<20)warnings.push('Small timing sample; percentiles are descriptive.');
    return{comparable:comparable,baseline:summary(before),current:summary(after),warnings:warnings};
  }
  function sortRuns(runs){return runs.slice().sort((a,b)=>{const x=scenarios.instant(a.startedAt),y=scenarios.instant(b.startedAt);return x===y?a.runId.localeCompare(b.runId):x>y?-1:1;});}
  return{readRun:readRun,readHistory:readHistory,summary:summary,compare:compare,sortRuns:sortRuns};
});

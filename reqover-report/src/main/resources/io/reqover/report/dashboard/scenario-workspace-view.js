/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
(function(){
  'use strict';
  if(!document.getElementById('scenario-workspace')){return;}
  const scenarios=window.ReqoverScenarios,results=window.ReqoverScenarioResults;
  let runs=[],historyInitialized=false;
  const byId=id=>document.getElementById('scenario-'+id);
  const el=(tag,text)=>{const node=document.createElement(tag);if(text!==undefined)node.textContent=String(text);return node;};
  const time=value=>value===null?'Not measured':value.toFixed(2)+' ms';
  function message(value){byId('message').textContent=value;}
  document.querySelectorAll('[data-scenario-view]').forEach(button=>button.addEventListener('click',()=>{
    ['cases','runs','recordings'].forEach(view=>byId(view+'-panel').hidden=view!==button.dataset.scenarioView);
    document.querySelectorAll('[data-scenario-view]').forEach(b=>b.setAttribute('aria-pressed',String(b===button)));
  }));
  function metadata(){return{name:byId('name').value,environment:byId('environment').value,revision:byId('revision').value};}
  function scenario(){return scenarios.fromDrafts(window.ReqoverCaseEditor.drafts(),metadata());}
  function download(name,value,type){const url=URL.createObjectURL(new Blob([value],{type:type}));const link=el('a');link.href=url;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
  function safe(action){try{action();}catch(error){message(error.message);}}
  function renderCases(){
    const values=window.ReqoverCaseEditor.drafts();
    const ready=values.filter(d=>window.ReqoverTestDrafts.problems(d).length===0).length;
    byId('count').textContent=values.length+' cases / '+ready+' reviewed and executable / '+(values.length-ready)+' require review or removal';
    byId('review').disabled=!values.length;
    let valid=false,saveable=false,problem='';try{const saved=scenario();saveable=true;scenarios.runnable(saved);valid=true;}catch(error){problem=error.message;}
    byId('validation').textContent=values.length?problem:'No test cases loaded.';
    byId('save').disabled=!saveable;
    ['postman','jmeter','command'].forEach(id=>byId(id).disabled=!valid);
  }
  byId('save').addEventListener('click',()=>safe(()=>{download('reqover-scenario.json',JSON.stringify(scenario(),null,2)+'\n','application/json');message('Scenario saved. Private paths may still need review before sharing.');}));
  byId('postman').addEventListener('click',()=>safe(()=>download('reqover.postman_collection.json',JSON.stringify(scenarios.postman(scenario()),null,2)+'\n','application/json')));
  byId('jmeter').addEventListener('click',()=>safe(()=>download('reqover.jmx',scenarios.jmeter(scenario()),'application/xml')));
  byId('command').addEventListener('click',()=>safe(()=>{scenarios.runnable(scenario());download('reqover-local-run.txt','java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar run --scenario reqover-scenario.json --base-url REPLACE_WITH_LOCAL_ORIGIN --confirm-local --history-dir .reqover/runs\n','text/plain');message('Command prepared with an unset target; no request executed by this page.');}));
  byId('review').addEventListener('click',()=>{window.location.hash='test-case-drafts';});
  ['name','environment','revision'].forEach(id=>byId(id).addEventListener('input',renderCases));
  window.addEventListener('reqover-drafts-changed',renderCases);
  byId('import').addEventListener('change',async()=>{
    const file=byId('import').files[0];if(!file){return;}
    try{if(file.size>1048576)throw new Error('Scenario exceeds 1 MiB.');const loaded=scenarios.read(JSON.parse(await file.text()));
      const incoming=scenarios.toDrafts(loaded);window.ReqoverCaseEditor.importDrafts(incoming);
      byId('name').value=loaded.name;byId('environment').value=loaded.environment;byId('revision').value=loaded.revision;renderCases();message('Scenario cases appended. Imported cases require review; existing edits preserved.');
    }catch(error){message('Import failed: '+error.message);}finally{byId('import').value='';}
  });
  function renderHistory(){
    const currentId=byId('current').value,baselineId=byId('baseline').value;
    ['current','baseline'].forEach(id=>{byId(id).replaceChildren(el('option','Not selected'));byId(id).firstChild.value='';runs.forEach(run=>{const option=el('option',run.scenario.name+' / '+run.startedAt+' / '+run.scenario.revision);option.value=run.runId;byId(id).append(option);});});
    const defaults=!historyInitialized&&runs.length>0;
    byId('current').value=defaults?runs[0].runId:runs.some(r=>r.runId===currentId)?currentId:'';
    byId('baseline').value=defaults?(runs[1]?.runId||''):runs.some(r=>r.runId===baselineId)?baselineId:'';
    if(defaults)historyInitialized=true;
    byId('history-count').textContent=runs.length+' loaded runs / maximum 20';byId('history-save').disabled=!runs.length;
    byId('history-rows').replaceChildren();const query=byId('history-filter').value.trim().toLowerCase();
    const visible=runs.filter(run=>[run.scenario.name,run.scenario.environment,run.scenario.revision,run.runId,run.origin].join(' ').toLowerCase().includes(query));
    if(!visible.length){const row=el('tr'),cell=el('td',runs.length?'No runs match this search.':'No runs loaded.');cell.setAttribute('colspan','5');row.append(cell);byId('history-rows').append(row);}
    visible.forEach(run=>{
      const s=results.summary(run),row=el('tr'),name=el('td',run.scenario.name);name.append(el('small',run.scenario.environment+' / '+run.scenario.revision));row.append(name,el('td',run.startedAt),el('td',s.passed+' / '+s.failed+' / '+s.notRun),el('td',time(s.averageMillis)+' / '+time(s.p95Millis)));
      const actions=el('td'),current=el('button','Current'),remove=el('button','Remove loaded run');current.type=remove.type='button';
      current.addEventListener('click',()=>{byId('current').value=run.runId;renderResults();});
      remove.addEventListener('click',()=>{runs=runs.filter(r=>r.runId!==run.runId);renderHistory();message('Removed from this page only. Disk history was not deleted.');});
      actions.append(current,remove);row.append(actions);byId('history-rows').append(row);
    });renderResults();
  }
  function renderResults(){
    byId('run-summary').replaceChildren();byId('comparison-warnings').replaceChildren();byId('comparison-rows').replaceChildren();byId('attempt-rows').replaceChildren();
    const current=runs.find(r=>r.runId===byId('current').value),selectedBaseline=runs.find(r=>r.runId===byId('baseline').value);
    if(!current){byId('attempt-count').textContent=runs.length?'Select a current run.':'No runs loaded.';return;}
    const sameRun=selectedBaseline?.runId===current.runId,baseline=sameRun?null:selectedBaseline;
    const s=results.summary(current);
    [['Passed',s.passed],['Failed assertions',s.failed],['Not run',s.notRun],['Client p95',time(s.p95Millis)]].forEach(pair=>{const metric=el('div');metric.append(el('span',pair[0]),el('strong',pair[1]));byId('run-summary').append(metric);});
    const warnings=baseline?results.compare(baseline,current).warnings:['Imported files are not authenticated execution evidence. Client attempt time is not server adapter time.'];
    if(sameRun)warnings.push('Select a different run as baseline; the current run cannot be compared with itself.');
    warnings.forEach(w=>byId('comparison-warnings').append(el('li',w)));
    if(baseline){const b=results.summary(baseline);[['Attempts',b.attempts,s.attempts],['Passed',b.passed,s.passed],['Failed assertions',b.failed,s.failed],['Client average',time(b.averageMillis),time(s.averageMillis)],['Client p95',time(b.p95Millis),time(s.p95Millis)]].forEach(values=>{const row=el('tr');values.forEach(v=>row.append(el('td',v)));byId('comparison-rows').append(row);});}
    byId('attempt-count').textContent=Math.min(100,current.attempts.length)+' of '+current.attempts.length+' attempt details / '+current.completion;
    current.attempts.slice(0,100).forEach(a=>{const row=el('tr');[a.caseId,a.actualStatus===null?'Unknown':a.actualStatus,time(a.elapsedMillis),a.outcome].forEach(value=>row.append(el('td',value)));byId('attempt-rows').append(row);});
  }
  byId('runs-import').addEventListener('change',async()=>{
    const files=Array.from(byId('runs-import').files);if(!files.length){return;}
    try{if(files.length>20)throw new Error('At most 20 files per import.');const candidate=new Map(runs.map(r=>[r.runId,r]));let omitted=0;
      for(const file of files){if(file.size>20971520)throw new Error('History input exceeds 20 MiB.');const data=JSON.parse(await file.text());omitted+=data.omittedRuns||0;for(const run of results.readHistory(data)){if(candidate.has(run.runId) && JSON.stringify(candidate.get(run.runId))!==JSON.stringify(run))throw new Error('Conflicting run ID.');candidate.set(run.runId,run);}}
      if(candidate.size>20)throw new Error('Remove loaded runs before exceeding 20.');runs=results.sortRuns(Array.from(candidate.values()));renderHistory();message('Runs loaded locally.'+(omitted?' Source catalog omitted '+omitted+' older runs.':''));
    }catch(error){message('Import failed; existing history preserved: '+error.message);}finally{byId('runs-import').value='';}
  });
  byId('history-save').addEventListener('click',()=>download('reqover-history.json',JSON.stringify({kind:'reqover-run-history',schemaVersion:1,omittedRuns:0,runs:runs},null,2)+'\n','application/json'));
  byId('history-filter').addEventListener('input',renderHistory);byId('current').addEventListener('change',renderResults);byId('baseline').addEventListener('change',renderResults);
  renderCases();renderHistory();
})();

/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
(function(){
  'use strict';const root=document.getElementById('recording-history-panel');if(!root)return;
  const api=window.ReqoverRecordingHistory;let entries=[];
  const get=id=>document.getElementById('recording-'+id);
  const el=(tag,text)=>{const node=document.createElement(tag);if(text!==undefined)node.textContent=String(text);return node;};
  function render(){
    const current=get('current').value,baseline=get('baseline').value;
    ['current','baseline'].forEach(id=>{get(id).replaceChildren(el('option','Not selected'));get(id).firstChild.value='';entries.forEach(e=>{const option=el('option',e.name+' / '+e.environment+' / '+e.revision);option.value=e.id;get(id).append(option);});});
    get('current').value=entries.some(e=>e.id===current)?current:entries[0]?.id||'';get('baseline').value=entries.some(e=>e.id===baseline)?baseline:entries[1]?.id||'';
    get('count').textContent=entries.length+' loaded recordings';get('save').disabled=!entries.length;get('rows').replaceChildren();
    entries.forEach(e=>{const row=el('tr'),name=el('td',e.name);name.append(el('small',e.environment+' / '+e.revision));const remove=el('button','Remove loaded');remove.type='button';remove.addEventListener('click',()=>{entries=entries.filter(item=>item.id!==e.id);render();get('message').textContent='Removed from this page only; disk archive unchanged.';});const action=el('td');action.append(remove);row.append(name,el('td',e.summary.generatedAt),el('td',e.summary.httpRequestCount+' observations / '+e.omittedRequestDetails+' omitted input details'),action);get('rows').append(row);});
    compare();
  }
  function compare(){get('comparison').replaceChildren();get('warnings').replaceChildren();const before=entries.find(e=>e.id===get('baseline').value),after=entries.find(e=>e.id===get('current').value);if(!before||!after)return;const comparison=api.compare(before,after);comparison.warnings.forEach(w=>get('warnings').append(el('li',w)));comparison.rows.slice(0,100).forEach(value=>{const row=el('tr');row.append(el('td',value.endpoint),el('td',value.averageDeltaMillis===null?'Not comparable':value.averageDeltaMillis.toFixed(2)+' ms'),el('td',value.p95DeltaMillis===null?'Not comparable':value.p95DeltaMillis.toFixed(2)+' ms'),el('td',value.failureDeltaPoints===null?'Not comparable':value.failureDeltaPoints.toFixed(2)+' pp'));get('comparison').append(row);});}
  get('import').addEventListener('change',async()=>{const files=Array.from(get('import').files);try{if(files.length>20)throw new Error('Maximum 20 files.');const next=new Map(entries.map(e=>[e.id,e]));for(const file of files){if(file.size>20971520)throw new Error('Recording input exceeds 20 MiB.');for(const e of api.read(JSON.parse(await file.text()))){if(next.has(e.id)&&JSON.stringify(next.get(e.id))!==JSON.stringify(e))throw new Error('Conflicting recording ID.');next.set(e.id,e);}}if(next.size>20)throw new Error('Maximum 20 loaded recordings.');entries=Array.from(next.values());render();get('message').textContent='Recording summaries loaded; raw detail data is not retained by this page.';}catch(error){get('message').textContent='Import failed; previous entries preserved: '+error.message;}finally{get('import').value='';}});
  get('save').addEventListener('click',()=>{const url=URL.createObjectURL(new Blob([JSON.stringify({kind:'reqover-recording-history',schemaVersion:1,recordings:entries},null,2)],{type:'application/json'}));const link=el('a');link.href=url;link.download='reqover-recordings.json';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);});
  get('current').addEventListener('change',compare);get('baseline').addEventListener('change',compare);render();
})();

/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
(function(root,factory){if(typeof module==='object' && module.exports)module.exports=factory(require('./recording-comparison.js'));else root.ReqoverRecordingHistory=factory(root.ReqoverRecordingComparison);})(typeof globalThis!=='undefined'?globalThis:this,function(comparison){
  'use strict';
  function entry(value){
    if(!value || value.kind!=='reqover-recording-entry' || value.schemaVersion!==1 || !/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i.test(value.id)){throw new Error('Expected recording entry schema 1.');}
    ['name','environment','revision','savedAt'].forEach(k=>{if(typeof value[k]!=='string' || value[k].length>120 || /[\u0000-\u001f\u007f-\u009f]/.test(value[k]))throw new Error('Invalid recording metadata.');});
    if(!Number.isFinite(Date.parse(value.savedAt)) || !Number.isSafeInteger(value.omittedRequestDetails) || value.omittedRequestDetails<0)throw new Error('Invalid recording boundary.');
    return{kind:value.kind,schemaVersion:1,id:value.id,name:value.name,environment:value.environment,revision:value.revision,savedAt:value.savedAt,omittedRequestDetails:value.omittedRequestDetails,summary:comparison.read(value.summary)};
  }
  function read(value){
    if(value.kind==='reqover-recording-entry')return[entry(value)];
    if(value.kind!=='reqover-recording-history' || value.schemaVersion!==1 || !Array.isArray(value.recordings) || value.recordings.length>20)throw new Error('Recording history supports at most 20 entries.');
    return value.recordings.map(entry);
  }
  function compare(before,after){
    before=entry(before);after=entry(after);const result=comparison.between(before.summary,after.summary);
    result.warnings=result.warnings.concat(['Imported recordings are not authenticated; confirm capture conditions.']);
    if(before.omittedRequestDetails || after.omittedRequestDetails)result.warnings.push('Input omitted request details; statistics cover only the archived detail window.');
    if(before.environment!==after.environment)result.warnings.push('Declared environments differ.');
    return result;
  }
  return{read:read,entry:entry,compare:compare};
});

/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) { module.exports = factory(require('./test-case-drafts.js')); }
  else { root.ReqoverScenarios = factory(root.ReqoverTestDrafts); }
})(typeof globalThis !== 'undefined' ? globalThis : this, function (drafts) {
  'use strict';
  function keys(object, allowed) {
    if (!object || typeof object !== 'object' || Array.isArray(object) || Object.keys(object).some(function (k) { return !allowed.includes(k); })) { throw new Error('Unsupported fields; credentials, headers and bodies are not allowed.'); }
  }
  function text(value, max, blank) {
    if (typeof value !== 'string' || value.length > max || (!blank && !value.trim()) || /[\u0000-\u001f\u007f-\u009f]/.test(value)) { throw new Error('Invalid scenario text.'); }
    return value;
  }
  function optionalNumber(value, min, max) {
    if (value === null) { return null; }
    if (!Number.isInteger(value) || value < min || value > max) { throw new Error('Invalid numeric test condition.'); }
    return value;
  }
  function instant(value) {
    if(typeof value!=='string')throw new Error('UTC instant required.');
    var match=/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))?Z$/.exec(value);
    if(!match)throw new Error('UTC instant required.');
    var date=new Date(match[1]+'Z');if(!Number.isFinite(date.getTime())||date.toISOString().slice(0,19)!==match[1])throw new Error('Invalid calendar date.');
    return BigInt(date.getTime())*1000000n+BigInt((match[2]||'').padEnd(9,'0'));
  }
  function read(value) {
    keys(value, ['kind','schemaVersion','name','environment','revision','savedAt','cases']);
    if (value.kind !== 'reqover-test-scenario' || value.schemaVersion !== 1) { throw new Error('Expected Reqover scenario schema 1.'); }
    if (!Array.isArray(value.cases) || !value.cases.length || value.cases.length > 100) { throw new Error('Scenario requires 1-100 cases.'); }
    var ids = new Set();
    var cases = value.cases.map(function (c) {
      keys(c, ['id','method','path','expectedStatus','maximumClientMillis','reviewed']);
      var id = text(c.id, 80, false); if (ids.has(id)) { throw new Error('Duplicate case ID.'); } ids.add(id);
      if (!['GET','HEAD','POST','PUT','PATCH','DELETE','OPTIONS'].includes(c.method)) { throw new Error('Invalid HTTP method.'); }
      var path = text(c.path, 2048, true);
      if (path && !drafts.validPath(path)) { throw new Error('Unsafe or unresolved scenario path.'); }
      if (typeof c.reviewed !== 'boolean') { throw new Error('Explicit review flag required.'); }
      return { id:id, method:c.method, path:path, expectedStatus:optionalNumber(c.expectedStatus,200,599),
        maximumClientMillis:optionalNumber(c.maximumClientMillis,1,600000), reviewed:c.reviewed };
    });
    var savedAt = text(value.savedAt, 50, false); instant(savedAt);
    return { kind:value.kind, schemaVersion:1, name:text(value.name,120,false), environment:text(value.environment,80,true),
      revision:text(value.revision,120,true), savedAt:savedAt, cases:cases };
  }
  function fromDrafts(values, metadata) {
    return read({ kind:'reqover-test-scenario', schemaVersion:1, name:metadata.name, environment:metadata.environment || '',
      revision:metadata.revision || '', savedAt:new Date().toISOString(), cases:values.map(function (d,i) { return {
        id:'case-' + (i+1), method:d.request.method, path:d.request.path, expectedStatus:d.expected.statusCode,
        maximumClientMillis:d.expected.maximumClientMillis, reviewed:d.reviewed && drafts.problems(d).length === 0 };
      }) });
  }
  function toDrafts(value) {
    var scenario = read(value);
    return scenario.cases.map(function (c) {
      var draft = drafts.create({ unitType:'http-request', endpoint:c.method + ' ' + (c.path || '/'),
        requestId:'scenario:' + c.id, startedAt:scenario.savedAt, endedAt:null, statusCode:-1, classes:[] });
      return drafts.update(draft, { path:c.path, expectedStatus:c.expectedStatus, maximumClientMillis:c.maximumClientMillis, reviewed:false });
    });
  }
  function runnable(value) {
    var scenario = read(value);
    if (scenario.cases.some(function (c) { return !c.reviewed || !['GET','HEAD'].includes(c.method) || !c.path || c.expectedStatus === null; })) {
      throw new Error('All cases must be reviewed GET/HEAD with concrete paths and expected status.');
    }
    return scenario;
  }
  function postman(value) {
    var s = runnable(value);
    return { info:{ name:s.name, schema:'https://schema.getpostman.com/json/collection/v2.1.0/collection.json', description:'Manually reviewed Reqover cases. Configure a disposable local/QA baseUrl. No captured authentication or faithful replay.' },
      variable:[{ key:'baseUrl', value:'', type:'string' }], protocolProfileBehavior:{ followRedirects:false },
      item:s.cases.map(function (c) {
        var script = ['pm.test("Expected status", function () { pm.response.to.have.status(' + c.expectedStatus + '); });'];
        if (c.maximumClientMillis !== null) { script.push('pm.test("Client time limit", function () { pm.expect(pm.response.responseTime).to.be.at.most(' + c.maximumClientMillis + '); });'); }
        return { name:c.id + ' ' + c.method + ' ' + c.path, request:{ method:c.method, header:[], url:'{{baseUrl}}' + c.path },
          event:[{ listen:'test', script:{ type:'text/javascript', exec:script } }] };
      }) };
  }
  function xml(value) { return String(value).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&apos;'); }
  function jmeter(value) {
    var s = runnable(value);
    // JMeter interprets ${...}; imported labels must remain literal text.
    s.name=s.name.replace(/\$\{/g,'$ {');
    s.cases.forEach(function(c){c.id=c.id.replace(/\$\{/g,'$ {');});
    var content = s.cases.map(function (c) {
      var response = '<ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Expected status" enabled="true"><collectionProp name="Asserion.test_strings"><stringProp name="0">' + c.expectedStatus + '</stringProp></collectionProp><stringProp name="Assertion.test_field">Assertion.response_code</stringProp><boolProp name="Assertion.assume_success">false</boolProp><intProp name="Assertion.test_type">8</intProp></ResponseAssertion><hashTree/>';
      if (c.maximumClientMillis !== null) { response += '<DurationAssertion guiclass="DurationAssertionGui" testclass="DurationAssertion" testname="Client time limit" enabled="true"><stringProp name="DurationAssertion.duration">' + c.maximumClientMillis + '</stringProp></DurationAssertion><hashTree/>'; }
      return '<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="' + xml(c.id) + '" enabled="true"><elementProp name="HTTPsampler.Arguments" elementType="Arguments"><collectionProp name="Arguments.arguments"/></elementProp><stringProp name="HTTPSampler.domain">${__P(reqover.host,)}</stringProp><stringProp name="HTTPSampler.port">${__P(reqover.port,80)}</stringProp><stringProp name="HTTPSampler.protocol">${__P(reqover.protocol,http)}</stringProp><stringProp name="HTTPSampler.path">' + xml(c.path) + '</stringProp><stringProp name="HTTPSampler.method">' + c.method + '</stringProp><boolProp name="HTTPSampler.follow_redirects">false</boolProp><boolProp name="HTTPSampler.auto_redirects">false</boolProp><boolProp name="HTTPSampler.use_keepalive">true</boolProp><stringProp name="HTTPSampler.connect_timeout">5000</stringProp><stringProp name="HTTPSampler.response_timeout">10000</stringProp></HTTPSamplerProxy><hashTree>' + response + '</hashTree>';
    }).join('');
    return '<?xml version="1.0" encoding="UTF-8"?><jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3"><hashTree><TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="' + xml(s.name) + '" enabled="false"><stringProp name="TestPlan.comments">Disabled by default. Set reqover.host explicitly; review disposable QA target before enabling.</stringProp><boolProp name="TestPlan.functional_mode">false</boolProp><boolProp name="TestPlan.serialize_threadgroups">true</boolProp><elementProp name="TestPlan.user_defined_variables" elementType="Arguments"><collectionProp name="Arguments.arguments"/></elementProp></TestPlan><hashTree><ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Reviewed checks" enabled="true"><stringProp name="ThreadGroup.on_sample_error">continue</stringProp><elementProp name="ThreadGroup.main_controller" elementType="LoopController"><boolProp name="LoopController.continue_forever">false</boolProp><stringProp name="LoopController.loops">1</stringProp></elementProp><stringProp name="ThreadGroup.num_threads">1</stringProp><stringProp name="ThreadGroup.ramp_time">1</stringProp><boolProp name="ThreadGroup.scheduler">false</boolProp></ThreadGroup><hashTree>' + content + '</hashTree></hashTree></hashTree></jmeterTestPlan>';
  }
  return { read:read, fromDrafts:fromDrafts, toDrafts:toDrafts, runnable:runnable, postman:postman, jmeter:jmeter, instant:instant };
});

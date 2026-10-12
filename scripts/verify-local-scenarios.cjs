// SPDX-License-Identifier: Apache-2.0
// End-to-end CLI checks against a disposable owned loopback server; no browser.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),assert=require('node:assert/strict');
const {execFile}=require('node:child_process');const {promisify}=require('node:util');const exec=promisify(execFile);
const drafts=require('../reqover-report/src/main/resources/io/reqover/report/dashboard/test-case-drafts.js');
const scenarios=require('../reqover-report/src/main/resources/io/reqover/report/dashboard/scenarios.js');
const results=require('../reqover-report/src/main/resources/io/reqover/report/dashboard/scenario-results.js');
(async()=>{
  const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java.exe'):'java';
  const jar=process.argv[2]||'reqover-cli/build/libs/reqover-cli-0.4.2.jar';const base=process.argv[3]||'tmp/scenario-workflow-check';fs.mkdirSync(base,{recursive:true});const out=fs.mkdtempSync(path.join(base,'session-'));
  let failing=true,requests=0;
  const server=http.createServer((req,res)=>{requests++;res.setHeader('Connection','close');res.statusCode=req.url==='/fail' && failing?503:200;res.end();});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  try{
    const values=['/ok','/fail'].map((p,i)=>drafts.update(drafts.create({unitType:'http-request',endpoint:'GET '+p,requestId:'r'+i,startedAt:'2026-10-12T00:00:00Z',classes:[]}),{path:p,expectedStatus:200,maximumClientMillis:5000,reviewed:true}));
    const scenario=scenarios.fromDrafts(values,{name:'Controlled local regression',environment:'fixture',revision:'before'});
    const input=path.join(out,'scenario.json'),history=path.join(out,'runs');fs.writeFileSync(input,JSON.stringify(scenario));
    fs.writeFileSync(path.join(out,'collection.json'),JSON.stringify(scenarios.postman(scenario)));fs.writeFileSync(path.join(out,'plan.jmx'),scenarios.jmeter(scenario));
    const origin='http://127.0.0.1:'+server.address().port;
    async function cli(args,expected){let response;try{response=await exec(java,['-jar',jar,...args],{windowsHide:true,timeout:90000});assert.equal(expected,0);}catch(error){assert.equal(error.code,expected);response=error;}return response.stdout;}
    await cli(['run','--scenario',input,'--base-url',origin,'--repeat','2','--history-dir',history],2);assert.equal(requests,0);
    await cli(['run','--scenario',input,'--base-url',origin,'--repeat','2','--history-dir',history,'--confirm-local'],1);assert.equal(requests,4);
    failing=false;scenario.revision='after';fs.writeFileSync(input,JSON.stringify(scenario));
    await cli(['run','--scenario',input,'--base-url',origin,'--repeat','2','--history-dir',history,'--confirm-local'],0);assert.equal(requests,8);
    const catalog=JSON.parse(await cli(['runs','--history-dir',history],0));assert.equal(catalog.runs.length,2);
    const before=results.readRun(catalog.runs.find(r=>r.scenario.revision==='before')),after=results.readRun(catalog.runs.find(r=>r.scenario.revision==='after'));
    assert.equal(results.summary(before).failed,2);assert.equal(results.summary(after).passed,4);assert.equal(results.compare(before,after).comparable,true);
    const files=fs.readdirSync(history).filter(f=>f.endsWith('.run.json'));const a=files.find(f=>JSON.parse(fs.readFileSync(path.join(history,f))).scenario.revision==='before'),b=files.find(f=>f!==a);
    await cli(['run-compare','--baseline',path.join(history,a),'--current',path.join(history,b),'--out',path.join(out,'comparison.html')],0);
    await cli(['run-report','--result',path.join(history,b),'--out',path.join(out,'result.html')],0);
    assert(fs.readFileSync(path.join(out,'result.html'),'utf8').includes('4 passed'));
    fs.writeFileSync(path.join(out,'history.json'),JSON.stringify(catalog,null,2));
    const coverage=path.join(out,'coverage.json'),recordings=path.join(out,'recordings');
    fs.writeFileSync(coverage,JSON.stringify({schemaVersion:1,generatedAt:'2026-10-12T00:00:01Z',completedRequestCount:1,endpoints:[{endpoint:'GET /ok',requestCount:1,requestIds:['r1'],threadNames:[],classes:[]}],reverseIndex:[],requests:[{requestId:'r1',unitType:'http-request',endpoint:'GET /ok',startedAt:'2026-10-12T00:00:00Z',endedAt:'2026-10-12T00:00:01Z',statusCode:200,threadNames:[],classes:[]}]}));
    await cli(['recording-save','--report',coverage,'--name','Named capture','--environment','fixture','--revision','rev1','--history-dir',recordings],0);
    const recordingCatalog=JSON.parse(await cli(['recordings','--history-dir',recordings],0));assert.equal(recordingCatalog.recordings.length,1);
    fs.writeFileSync(path.join(out,'recording-catalog.json'),JSON.stringify(recordingCatalog,null,2));
    console.log(JSON.stringify({localRequests:requests,consentGuard:true,firstRunExit:1,secondRunExit:0,history:2,recordingHistory:1,assertionsAndComparison:true,postmanAndJmxExported:true,outputDirectory:out,browserOpened:false}));
  }finally{await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exit(1);});

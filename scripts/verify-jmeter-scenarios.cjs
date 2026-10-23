// SPDX-License-Identifier: Apache-2.0
// Real JMeter headless format validation, against an owned loopback fixture only.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),assert=require('node:assert/strict');
const {promisify}=require('node:util'),{execFile}=require('node:child_process');const exec=promisify(execFile);
const scenarios=require('../reqover-report/src/main/resources/io/reqover/report/dashboard/scenarios.js');
(async()=>{
  const [jmeter,python]=process.argv.slice(2);assert(jmeter&&python,'Supply verified JMeter JAR and Python interpreter.');
  fs.mkdirSync('tmp',{recursive:true});const out=fs.mkdtempSync(path.join('tmp','jmeter-scenario-check-'));let requests=0;
  const server=http.createServer((req,res)=>{requests++;res.setHeader('Connection','close');res.statusCode=req.url==='/fail'?503:200;res.end();});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  try{
    const scenario={kind:'reqover-test-scenario',schemaVersion:1,name:'Fixture',environment:'local',revision:'',savedAt:'2026-10-12T00:00:00Z',cases:['/ok','/fail'].map((p,i)=>({id:'case-'+i,method:'GET',path:p,expectedStatus:200,maximumClientMillis:5000,reviewed:true}))};
    const xml=scenarios.jmeter(scenario);fs.writeFileSync(path.join(out,'disabled.jmx'),xml);
    const java=path.join(process.env.JAVA_HOME,'bin','java.exe');
    async function engine(file,name,disabled=false){try{await exec(java,['-Xmx256m','-jar',jmeter,'-n','-t',file,'-l',path.join(out,name+'.jtl'),'-j',path.join(out,name+'.log'),'-Jreqover.host=127.0.0.1','-Jreqover.port='+server.address().port,'-Jjmeter.save.saveservice.output_format=xml'],{windowsHide:true,timeout:120000});}catch(error){if(!disabled||error.code!==1)throw error;}}
    await engine(path.join(out,'disabled.jmx'),'disabled',true);assert.equal(requests,0,'A disabled exported plan must not send traffic even when a host is supplied');
    fs.writeFileSync(path.join(out,'reviewed.jmx'),xml.replaceAll('enabled="false"','enabled="true"'));
    await engine(path.join(out,'reviewed.jmx'),'reviewed');assert.equal(requests,2);
    const result=await exec(python,['-c','import xml.etree.ElementTree as E,json,sys; samples=list(E.parse(sys.argv[1]).getroot()); print(json.dumps({"samples":len(samples),"passed":sum(s.attrib.get("s")=="true" for s in samples)}))',path.join(out,'reviewed.jtl')],{windowsHide:true});
    const counts=JSON.parse(result.stdout);assert.equal(counts.samples,2);assert.equal(counts.passed,1);
    console.log(JSON.stringify({jmeter:'5.6.3',disabledSends:0,reviewedRequests:requests,statusAssertions:true,counts,browserOpened:false}));
  }finally{await new Promise(resolve=>server.close(resolve));}
})().catch(e=>{console.error(e);process.exit(1);});

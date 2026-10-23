/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.report;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Credential-free client check results. Imported files are not authenticated evidence. */
public record ScenarioRun(String runId, TestScenario scenario, String origin, String startedAt, String endedAt,
                          int plannedAttempts, String completion, List<Attempt> attempts) {
    public record Attempt(String caseId, Integer actualStatus, double elapsedMillis, String outcome) { }
    private static final Set<String> OUTCOMES = Set.of("PASS","STATUS_MISMATCH","CLIENT_LIMIT_EXCEEDED","NETWORK_ERROR","TIMEOUT","INTERRUPTED");
    public ScenarioRun {
        java.util.UUID.fromString(runId); Instant.parse(startedAt); Instant.parse(endedAt);
        TestScenario.text(origin,200,false);
        var target=java.net.URI.create(origin);
        if(!Set.of("http","https").contains(target.getScheme()) || target.getHost()==null || target.getRawUserInfo()!=null || target.getRawQuery()!=null || target.getRawFragment()!=null || !target.getRawPath().isEmpty())throw new IllegalArgumentException("Invalid credential-free run origin");
        scenario.requireRunnable(); attempts = List.copyOf(attempts);
        if(plannedAttempts < 1 || plannedAttempts > 1000 || attempts.size() > plannedAttempts) throw new IllegalArgumentException("Invalid attempt bounds");
        if(!Set.of("COMPLETE","BUDGET_EXHAUSTED","INTERRUPTED").contains(completion)) throw new IllegalArgumentException("Invalid completion");
        if(completion.equals("COMPLETE") && attempts.size()!=plannedAttempts)throw new IllegalArgumentException("Incomplete attempts marked complete");
        for(Attempt a:attempts) {
            var c = scenario.cases().stream().filter(value -> value.id().equals(a.caseId())).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown case ID"));
            if(!Double.isFinite(a.elapsedMillis()) || a.elapsedMillis()<0 || a.elapsedMillis()>86400000 || !OUTCOMES.contains(a.outcome())) throw new IllegalArgumentException("Invalid client result");
            if(a.actualStatus()!=null && (a.actualStatus()<100 || a.actualStatus()>599)) throw new IllegalArgumentException("Invalid actual status");
            if("PASS".equals(a.outcome()) && (!c.expectedStatus().equals(a.actualStatus()) || c.maximumClientMillis()!=null && a.elapsedMillis()>c.maximumClientMillis())) throw new IllegalArgumentException("Inconsistent passing result");
        }
    }
    public int passedCount() { return (int)attempts.stream().filter(a -> a.outcome().equals("PASS")).count(); }
    public Double averageMillis() { return attempts.isEmpty()?null:attempts.stream().mapToDouble(Attempt::elapsedMillis).average().orElseThrow(); }
    public Double p95Millis() {
        var values = attempts.stream().map(Attempt::elapsedMillis).sorted().toList();
        return values.isEmpty()?null:values.get((int)Math.ceil(values.size()*.95)-1);
    }
    public String json() {
        StringBuilder out = new StringBuilder("{\"kind\":\"reqover-scenario-run\",\"schemaVersion\":1,\"runId\":"); Json.writeString(out,runId);
        out.append(",\"scenario\":").append(scenario.json()).append(",\"fingerprint\":");Json.writeString(out,scenario.fingerprint());
        out.append(",\"origin\":");Json.writeString(out,origin);out.append(",\"startedAt\":");Json.writeString(out,startedAt);out.append(",\"endedAt\":");Json.writeString(out,endedAt);
        out.append(",\"plannedAttempts\":").append(plannedAttempts).append(",\"completion\":");Json.writeString(out,completion);out.append(",\"attempts\":[");
        boolean comma=false; for(Attempt a:attempts) {
            if(comma)out.append(',');comma=true;out.append("{\"caseId\":");Json.writeString(out,a.caseId());out.append(",\"actualStatus\":").append(a.actualStatus())
              .append(",\"elapsedMillis\":").append(a.elapsedMillis()).append(",\"outcome\":");Json.writeString(out,a.outcome());out.append('}');
        }
        return out.append("]}").toString();
    }
    public static ScenarioRun read(String json) {
        if(json.length()>1_048_576)throw new IllegalArgumentException("Run file exceeds 1 MiB");
        var root=Json.object(Json.parse(json),"run");
        TestScenario.keys(root,Set.of("kind","schemaVersion","runId","scenario","fingerprint","origin","startedAt","endedAt","plannedAttempts","completion","attempts"));
        if(!"reqover-scenario-run".equals(root.get("kind")) || !Integer.valueOf(1).equals(TestScenario.number(root,"schemaVersion")))throw new IllegalArgumentException("Expected run schema 1");
        var rawScenario=Json.object(root.get("scenario"),"scenario");
        // Serialize only this structured scenario using the same validated shape.
        TestScenario.keys(rawScenario,Set.of("kind","schemaVersion","name","environment","revision","savedAt","cases"));
        if(!"reqover-test-scenario".equals(rawScenario.get("kind")) || !Integer.valueOf(1).equals(TestScenario.number(rawScenario,"schemaVersion")))throw new IllegalArgumentException("Invalid nested scenario");
        var cases=Json.optionalArray(rawScenario,"cases").stream().map(item -> {
            var c=Json.object(item,"case");TestScenario.keys(c,Set.of("id","method","path","expectedStatus","maximumClientMillis","reviewed"));
            if(!(c.get("reviewed") instanceof Boolean))throw new IllegalArgumentException("Review flag required");
            return new TestScenario.Case(Json.string(c,"id"),Json.string(c,"method"),Json.string(c,"path"),TestScenario.number(c,"expectedStatus"),TestScenario.number(c,"maximumClientMillis"),(Boolean)c.get("reviewed"));
        }).toList();
        var scenario=new TestScenario(Json.string(rawScenario,"name"),Json.string(rawScenario,"environment"),Json.string(rawScenario,"revision"),Json.string(rawScenario,"savedAt"),cases);
        if(!scenario.fingerprint().equals(Json.string(root,"fingerprint")))throw new IllegalArgumentException("Scenario fingerprint mismatch");
        var attempts=Json.optionalArray(root,"attempts").stream().map(item -> {
            var a=Json.object(item,"attempt");TestScenario.keys(a,Set.of("caseId","actualStatus","elapsedMillis","outcome"));
            if(!(a.get("elapsedMillis") instanceof Number n))throw new IllegalArgumentException("Invalid elapsed time");
            return new Attempt(Json.string(a,"caseId"),TestScenario.number(a,"actualStatus"),n.doubleValue(),Json.string(a,"outcome"));
        }).toList();
        Integer planned=TestScenario.number(root,"plannedAttempts"); if(planned==null)throw new IllegalArgumentException("Missing planned count");
        return new ScenarioRun(Json.string(root,"runId"),scenario,Json.string(root,"origin"),Json.string(root,"startedAt"),Json.string(root,"endedAt"),planned,Json.string(root,"completion"),attempts);
    }
    public String html() {
        StringBuilder rows=new StringBuilder();
        for(Attempt a:attempts)rows.append("<tr><td>").append(escape(a.caseId())).append("</td><td>").append(a.actualStatus()==null?"Unknown":a.actualStatus())
            .append("</td><td>").append(String.format(java.util.Locale.ROOT,"%.2f",a.elapsedMillis())).append(" ms</td><td>").append(escape(a.outcome())).append("</td></tr>");
        return page("Scenario run", "<h1>"+escape(scenario.name())+"</h1><p>"+escape(scenario.environment())+" / revision "+escape(scenario.revision())+" / "+escape(origin)+"</p><p>"+escape(startedAt)+" / "+escape(completion)+"</p><p>"+passedCount()+" passed / "+(attempts.size()-passedCount())+" failed / "+(plannedAttempts-attempts.size())+" not run</p><p>Client attempt average: "+averageMillis()+" ms / p95: "+p95Millis()+" ms. Small samples are descriptive, not a performance benchmark.</p><table><thead><tr><th>Case</th><th>Actual HTTP</th><th>Client elapsed</th><th>Assertion result</th></tr></thead><tbody>"+rows+"</tbody></table>");
    }
    public static String compare(ScenarioRun before,ScenarioRun after) {
        boolean comparable=before.scenario.fingerprint().equals(after.scenario.fingerprint()) && before.origin.equals(after.origin) && before.scenario.environment().equals(after.scenario.environment());
        return page("Run comparison","<h1>Run comparison</h1><p>"+(comparable?"Matching declared scenario, origin and environment; manually verify conditions.":"Runs are not comparable: scenario, origin or environment differs.")+"</p><p>No automatic performance verdict. Files are not authenticated execution evidence.</p><table><tr><th></th><th>Baseline</th><th>Current</th></tr><tr><td>Run</td><td>"+escape(before.runId)+"</td><td>"+escape(after.runId)+"</td></tr><tr><td>Revision</td><td>"+escape(before.scenario.revision())+"</td><td>"+escape(after.scenario.revision())+"</td></tr><tr><td>Attempts</td><td>"+before.attempts.size()+"</td><td>"+after.attempts.size()+"</td></tr><tr><td>Passed</td><td>"+before.passedCount()+"</td><td>"+after.passedCount()+"</td></tr><tr><td>Client average ms</td><td>"+before.averageMillis()+"</td><td>"+after.averageMillis()+"</td></tr><tr><td>Client p95 ms</td><td>"+before.p95Millis()+"</td><td>"+after.p95Millis()+"</td></tr></table>");
    }
    private static String escape(String s) { return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
    private static String page(String title,String body) {
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"+title+"</title><style>body{font:14px system-ui;color:#20272b;background:#f6f7f9;margin:0;padding:24px}main{max-width:1000px;margin:auto}h1{font-size:24px}table{width:100%;border-collapse:collapse;background:white}td,th{text-align:left;padding:12px;border-bottom:1px solid #dce2e6;overflow-wrap:anywhere}p{overflow-wrap:anywhere;line-height:1.6}</style></head><body><main>"+body+"</main></body></html>";
    }
}

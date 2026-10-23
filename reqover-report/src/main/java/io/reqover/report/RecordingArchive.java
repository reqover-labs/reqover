/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.report;

/** Named canonical coverage snapshot plus an explicit exported-detail boundary. */
public record RecordingArchive(String id,String name,String environment,String revision,String savedAt,String reportJson,int omittedRequestDetails) {
    public RecordingArchive {
        java.util.UUID.fromString(id);name=TestScenario.text(name,120,false);environment=TestScenario.text(environment,80,true);revision=TestScenario.text(revision,120,true);java.time.Instant.parse(savedAt);
        if(omittedRequestDetails<0)throw new IllegalArgumentException("Invalid omitted detail count");
        var report=CoverageReportJson.read(reportJson);
        if(report.requests().size()>10000)throw new IllegalArgumentException("Archive supports at most 10000 detail records");
        reportJson=CoverageReportJson.write(report,10000);
    }
    public static RecordingArchive create(String json,String name,String environment,String revision) {
        var root=Json.object(Json.parse(json),"report");Integer omitted=TestScenario.number(root,"omittedRequestDetails");
        return new RecordingArchive(java.util.UUID.randomUUID().toString(),name,environment,revision,java.time.Instant.now().toString(),json,omitted==null?0:omitted);
    }
    public String catalogJson() {
        StringBuilder out=new StringBuilder("{\"kind\":\"reqover-recording-entry\",\"schemaVersion\":1,\"id\":");Json.writeString(out,id);
        out.append(",\"name\":");Json.writeString(out,name);out.append(",\"environment\":");Json.writeString(out,environment);out.append(",\"revision\":");Json.writeString(out,revision);out.append(",\"savedAt\":");Json.writeString(out,savedAt);
        return out.append(",\"omittedRequestDetails\":").append(omittedRequestDetails).append(",\"summary\":").append(RecordingComparisonHtmlRenderer.summaryJson(CoverageReportJson.read(reportJson))).append('}').toString();
    }
    public String json() { String catalog=catalogJson();StringBuilder out=new StringBuilder(catalog.substring(0,catalog.length()-1)).append(",\"reportJson\":");Json.writeString(out,reportJson);return out.append('}').toString(); }
    public String html() {
        String html=new HtmlCoverageReportRenderer().render(CoverageReportJson.read(reportJson));
        String context="<p class=\"api-scope\">Archived recording: "+escape(name)+" / "+escape(environment)+" / revision "+escape(revision)+" / source omitted details: "+omittedRequestDetails+". Statistics cover the archived detail window.</p>";
        return html.replace("<main class=\"wrap\">","<main class=\"wrap\">"+context);
    }
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    public static RecordingArchive read(String json) {
        var root=Json.object(Json.parse(json),"recording archive");TestScenario.keys(root,java.util.Set.of("kind","schemaVersion","id","name","environment","revision","savedAt","omittedRequestDetails","summary","reportJson"));
        if(!"reqover-recording-entry".equals(root.get("kind")) || !Integer.valueOf(1).equals(TestScenario.number(root,"schemaVersion")))throw new IllegalArgumentException("Expected recording entry schema 1");
        Integer omitted=TestScenario.number(root,"omittedRequestDetails");if(omitted==null)throw new IllegalArgumentException("Missing omitted detail count");
        return new RecordingArchive(Json.string(root,"id"),Json.string(root,"name"),Json.string(root,"environment"),Json.string(root,"revision"),Json.string(root,"savedAt"),Json.string(root,"reportJson"),omitted);
    }
}

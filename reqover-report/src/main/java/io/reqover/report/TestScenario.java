/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.report;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Credential-free manually specified test conditions, not captured request replay. */
public record TestScenario(String name, String environment, String revision, String savedAt, List<Case> cases) {
    public record Case(String id, String method, String path, Integer expectedStatus, Integer maximumClientMillis, boolean reviewed) { }
    public TestScenario {
        name = text(name,120,false); environment = text(environment,80,true); revision = text(revision,120,true);
        savedAt = text(savedAt,50,false); Instant.parse(savedAt);
        cases = List.copyOf(cases);
        if (cases.isEmpty() || cases.size() > 100) { throw new IllegalArgumentException("Scenario requires 1-100 cases"); }
        Set<String> ids = new HashSet<>();
        for (Case c : cases) {
            if (!ids.add(text(c.id(),80,false))) { throw new IllegalArgumentException("Duplicate case ID"); }
            if (!Set.of("GET","HEAD","POST","PUT","PATCH","DELETE","OPTIONS").contains(c.method())) { throw new IllegalArgumentException("Invalid HTTP method"); }
            text(c.path(),2048,true); if (!c.path().isEmpty() && !validPath(c.path())) { throw new IllegalArgumentException("Unsafe or unresolved scenario path"); }
            if (c.expectedStatus() != null && (c.expectedStatus() < 200 || c.expectedStatus() > 599)) { throw new IllegalArgumentException("Invalid expected HTTP status"); }
            if (c.maximumClientMillis() != null && (c.maximumClientMillis() < 1 || c.maximumClientMillis() > 600000)) { throw new IllegalArgumentException("Invalid client time limit"); }
        }
    }
    public void requireRunnable() {
        if (cases.stream().anyMatch(c -> !c.reviewed() || !Set.of("GET","HEAD").contains(c.method()) || c.path().isEmpty() || c.expectedStatus() == null)) {
            throw new IllegalArgumentException("All cases must be reviewed GET/HEAD with paths and expected status");
        }
    }
    public static boolean validPath(String path) {
        if (!path.startsWith("/") || path.startsWith("//")) { return false; }
        try {
            String decoded = URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
            if (decoded.startsWith("//") || decoded.codePoints().anyMatch(c -> Character.isWhitespace(c) || c < 32 || (c >= 127 && c <= 159) || "\\?#{}*<>\"[]^`|".indexOf(c) >= 0)) { return false; }
            for (String segment : decoded.split("/")) { if (segment.equals(".") || segment.equals("..")) { return false; } }
            return URI.create(path).getRawQuery() == null;
        } catch (IllegalArgumentException e) { return false; }
    }
    static String text(String value, int max, boolean blank) {
        if (value == null || value.length() > max || (!blank && value.isBlank()) || value.codePoints().anyMatch(c -> c < 32 || c >= 127 && c <= 159)) { throw new IllegalArgumentException("Invalid scenario text"); }
        return value;
    }
    static void keys(Map<String,Object> value, Set<String> allowed) {
        if (!allowed.containsAll(value.keySet())) { throw new IllegalArgumentException("Unsupported fields; secrets, headers and bodies are not allowed"); }
    }
    static Integer number(Map<String,Object> value, String key) {
        Object n = value.get(key); if (n == null) { return null; }
        if (!(n instanceof Number number) || !Double.isFinite(number.doubleValue()) || number.doubleValue() != Math.rint(number.doubleValue()) || Math.abs(number.doubleValue()) > Integer.MAX_VALUE) { throw new IllegalArgumentException("Invalid numeric condition"); }
        return number.intValue();
    }
    public static TestScenario read(String json) {
        if (json.length() > 1_048_576) { throw new IllegalArgumentException("Scenario exceeds 1 MiB"); }
        var root = Json.object(Json.parse(json), "scenario");
        keys(root,Set.of("kind","schemaVersion","name","environment","revision","savedAt","cases"));
        if (!"reqover-test-scenario".equals(root.get("kind")) || !Integer.valueOf(1).equals(number(root,"schemaVersion"))) { throw new IllegalArgumentException("Expected scenario schema 1"); }
        var cases = Json.optionalArray(root,"cases").stream().map(item -> {
            var c = Json.object(item,"case"); keys(c,Set.of("id","method","path","expectedStatus","maximumClientMillis","reviewed"));
            if (!(c.get("reviewed") instanceof Boolean)) { throw new IllegalArgumentException("Review flag required"); }
            return new Case(Json.string(c,"id"),Json.string(c,"method"),Json.string(c,"path"),number(c,"expectedStatus"),number(c,"maximumClientMillis"),(Boolean)c.get("reviewed"));
        }).toList();
        return new TestScenario(Json.string(root,"name"),Json.string(root,"environment"),Json.string(root,"revision"),Json.string(root,"savedAt"),cases);
    }
    public String json() {
        StringBuilder out = new StringBuilder("{\"kind\":\"reqover-test-scenario\",\"schemaVersion\":1,\"name\":"); Json.writeString(out,name);
        out.append(",\"environment\":"); Json.writeString(out,environment); out.append(",\"revision\":"); Json.writeString(out,revision);
        out.append(",\"savedAt\":"); Json.writeString(out,savedAt); out.append(",\"cases\":[");
        boolean comma=false; for (Case c:cases) {
            if(comma)out.append(','); comma=true; out.append("{\"id\":");Json.writeString(out,c.id());out.append(",\"method\":");Json.writeString(out,c.method());out.append(",\"path\":");Json.writeString(out,c.path());
            out.append(",\"expectedStatus\":").append(c.expectedStatus()).append(",\"maximumClientMillis\":").append(c.maximumClientMillis()).append(",\"reviewed\":").append(c.reviewed()).append('}');
        }
        return out.append("]}").toString();
    }
    public String fingerprint() {
        try {
            var canonical = new TestScenario("manifest","","","1970-01-01T00:00:00Z",cases);
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(canonical.json().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}

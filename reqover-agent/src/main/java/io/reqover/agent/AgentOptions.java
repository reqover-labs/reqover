package io.reqover.agent;

import java.util.ArrayList;
import java.util.List;

public record AgentOptions(
        List<String> includes,
        List<String> excludes
) {
    private static final List<String> DEFAULT_EXCLUDES = List.of(
            "java.",
            "javax.",
            "jakarta.",
            "jdk.",
            "sun.",
            "com.sun.",
            "org.springframework.",
            "reactor.",
            "io.micrometer.",
            "org.objectweb.asm.",
            "io.reqover.core.",
            "io.reqover.agent.",
            "io.reqover.instrumentation.",
            "io.reqover.report.",
            "io.reqover.spring."
    );

    public AgentOptions {
        includes = List.copyOf(includes);
        excludes = List.copyOf(excludes);
    }

    public static AgentOptions parse(String args) {
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>(DEFAULT_EXCLUDES);

        if (args == null || args.isBlank()) {
            return new AgentOptions(includes, excludes);
        }

        for (String part : args.split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String key = pair[0].trim();
            String value = pair[1].trim();
            if (value.isBlank()) {
                continue;
            }
            if ("include".equals(key)) {
                includes.addAll(prefixes(value));
            } else if ("exclude".equals(key)) {
                excludes.addAll(prefixes(value));
            }
        }

        return new AgentOptions(includes, excludes);
    }

    public boolean shouldInstrument(String dottedClassName) {
        for (String exclude : excludes) {
            if (dottedClassName.startsWith(exclude)) {
                return false;
            }
        }
        if (includes.isEmpty()) {
            return true;
        }
        for (String include : includes) {
            if (dottedClassName.startsWith(include)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> prefixes(String value) {
        List<String> prefixes = new ArrayList<>();
        for (String item : value.split(";")) {
            String prefix = item.trim();
            if (!prefix.isBlank()) {
                prefixes.add(prefix);
            }
        }
        return prefixes;
    }
}

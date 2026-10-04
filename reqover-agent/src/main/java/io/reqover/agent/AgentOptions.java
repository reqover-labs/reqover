package io.reqover.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed {@code -javaagent} options.
 *
 * <p>Syntax: {@code include=com.example;org.demo,exclude=com.example.generated}.
 * Prefixes are matched against dotted class names, and the most specific
 * (longest) matching prefix wins, so an explicit include may carve out a
 * subpackage of a default-excluded framework prefix. JDK, ASM, and Reqover
 * runtime packages are always excluded and cannot be enabled by an include.
 * On a tie the exclude wins.
 *
 * <p>Proxy classes generated at runtime (Spring CGLIB, Hibernate, Byte Buddy)
 * are also always excluded. They live in the application's package, so an
 * include matches them, but their methods only delegate to the real method,
 * which is instrumented on its own. Recording both lists every proxied bean
 * twice in the report.
 */
public record AgentOptions(
        List<String> includes,
        List<String> excludes
) {
    private static final List<String> HARD_EXCLUDES = List.of(
            "java.",
            "javax.",
            "jakarta.",
            "jdk.",
            "sun.",
            "com.sun.",
            "org.objectweb.asm.",
            "io.reqover.core.",
            "io.reqover.agent.",
            "io.reqover.instrumentation.",
            "io.reqover.report.",
            "io.reqover.spring."
    );

    private static final List<String> GENERATED_PROXY_MARKERS = List.of(
            "$$SpringCGLIB$$",
            "$$EnhancerBySpringCGLIB$$",
            "$$FastClassBySpringCGLIB$$",
            "$$EnhancerByCGLIB$$",
            "$HibernateProxy$",
            "$ByteBuddy$"
    );

    private static final List<String> DEFAULT_EXCLUDES = List.of(
            "org.springframework.",
            "reactor.",
            "io.micrometer."
    );

    public AgentOptions {
        includes = List.copyOf(includes);
        excludes = List.copyOf(excludes);
    }

    public static AgentOptions parse(String args) {
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>(DEFAULT_EXCLUDES);

        if (args == null || args.isBlank()) {
            warn("no include configured; instrumentation is disabled (use include=com.example.app)");
            return new AgentOptions(includes, excludes);
        }

        for (String part : args.split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                warn("ignoring malformed agent option \"" + part.trim() + "\" (expected key=value)");
                continue;
            }
            String key = pair[0].trim();
            String value = pair[1].trim();
            if (value.isBlank()) {
                warn("ignoring agent option \"" + key + "\" with empty value");
                continue;
            }
            if ("include".equals(key)) {
                includes.addAll(prefixes(value));
            } else if ("exclude".equals(key)) {
                excludes.addAll(prefixes(value));
            } else {
                warn("ignoring unknown agent option \"" + key + "\"");
            }
        }

        if (includes.isEmpty()) {
            warn("no valid include configured; instrumentation is disabled (use include=com.example.app)");
        }
        return new AgentOptions(includes, excludes);
    }

    public boolean shouldInstrument(String dottedClassName) {
        if (includes.isEmpty()
                || longestMatch(HARD_EXCLUDES, dottedClassName) >= 0
                || isGeneratedProxy(dottedClassName)) {
            return false;
        }
        int longestExclude = longestMatch(excludes, dottedClassName);
        return longestMatch(includes, dottedClassName) > longestExclude;
    }

    private static boolean isGeneratedProxy(String dottedClassName) {
        for (String marker : GENERATED_PROXY_MARKERS) {
            if (dottedClassName.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static int longestMatch(List<String> prefixes, String dottedClassName) {
        int longest = -1;
        for (String prefix : prefixes) {
            if (dottedClassName.startsWith(prefix) && prefix.length() > longest) {
                longest = prefix.length();
            }
        }
        return longest;
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

    private static void warn(String message) {
        System.err.println("[reqover] " + message);
    }
}

package io.reqover.report;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * One method, identified the way the report records it.
 *
 * <p>Used by the diff and impact analyses, which compare code across two
 * reports and against changed files rather than reading a single report.
 */
public record CodeRef(String className, String methodName, String descriptor) {
    public CodeRef {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(methodName, "methodName");
        descriptor = Objects.requireNonNullElse(descriptor, "");
    }

    static CodeRef of(CodeEndpointCoverage coverage) {
        return new CodeRef(coverage.className(), coverage.methodName(), coverage.descriptor());
    }

    /** {@code com.example.OrderService#find(long): OrderResponse}. */
    public String display() {
        return className + "#" + methodName + HtmlCoverageReportRenderer.readableSignature(descriptor);
    }

    /** Stable key for set membership and sorting. */
    public String key() {
        return className + "#" + methodName + descriptor;
    }

    /**
     * The source files this class could have been declared in, as repository
     * paths — {@code a.b.Outer$Inner} becomes {@code a/b/Outer.java}.
     *
     * <p>A Kotlin variant is included because a changed {@code .kt} file
     * produces JVM classes under the same package path. Secondary top-level
     * classes declared in a differently named file are not matched; that is a
     * known miss, not a silent one, because the file then lands in the
     * unmatched list.
     */
    public Set<String> sourceFileSuffixes() {
        String binary = className.replace('.', '/');
        int lastSlash = binary.lastIndexOf('/');
        String simpleName = lastSlash < 0 ? binary : binary.substring(lastSlash + 1);

        int nested = simpleName.indexOf('$');
        if (nested >= 0) {
            simpleName = simpleName.substring(0, nested);
        }
        if (simpleName.isEmpty()) {
            return Set.of();
        }

        String base = (lastSlash < 0 ? "" : binary.substring(0, lastSlash + 1)) + simpleName;
        Set<String> suffixes = new LinkedHashSet<>();
        suffixes.add(base + ".java");
        suffixes.add(base + ".kt");
        return suffixes;
    }
}

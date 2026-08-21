package io.reqover.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for serving and exporting the report, bound from
 * {@code reqover.report.*}.
 */
@ConfigurationProperties(prefix = "reqover.report")
public class ReqoverReportProperties {
    private final Endpoint endpoint = new Endpoint();
    private final Export export = new Export();

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Export getExport() {
        return export;
    }

    /**
     * The built-in HTTP report endpoint.
     *
     * <p>It is off by default and should stay off in any environment that is
     * reachable by someone who should not read it: the report names internal
     * classes and methods, and Reqover deliberately ships no authentication of
     * its own. Turning it on is a decision about your deployment, so guard the
     * path with your own security configuration when you do.
     */
    public static class Endpoint {
        private boolean enabled = false;
        private String path = "/reqover/report";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Base path for the endpoint. JSON is served here and the HTML report
         * at the same path with {@code .html} appended.
         */
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * Writing the report to disk when the application shuts down, which is how
     * a CI job gets a report out of an integration test run.
     *
     * <p>The export runs on normal context shutdown. A process killed with
     * {@code SIGKILL} writes nothing.
     */
    public static class Export {
        private String jsonPath;
        private String htmlPath;

        /** Where to write the JSON report, or {@code null} to skip it. */
        public String getJsonPath() {
            return jsonPath;
        }

        public void setJsonPath(String jsonPath) {
            this.jsonPath = jsonPath;
        }

        /** Where to write the HTML report, or {@code null} to skip it. */
        public String getHtmlPath() {
            return htmlPath;
        }

        public void setHtmlPath(String htmlPath) {
            this.htmlPath = htmlPath;
        }

        public boolean isEnabled() {
            return hasText(jsonPath) || hasText(htmlPath);
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}

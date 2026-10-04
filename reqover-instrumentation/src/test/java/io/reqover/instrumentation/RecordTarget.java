package io.reqover.instrumentation;

public record RecordTarget(String code, int status) {
    public String label() {
        return code + ":" + status;
    }
}

package io.reqover.instrumentation;

public class AccessorTarget {
    private String name;
    private boolean active;
    private long count;
    private AccessorTarget parent;

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getDisplayName() {
        return name == null ? "anonymous" : name;
    }

    public String getParentName() {
        return parent.name;
    }

    public long getCountPlusOne() {
        return count + 1;
    }

    public AccessorTarget withName(String name) {
        this.name = name;
        return this;
    }

    public AccessorTarget withCountChecked(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count");
        }
        this.count = count;
        return this;
    }

    public static String $default$name() {
        return "anonymous";
    }

    public void setNameTrimmed(String name) {
        this.name = name.trim();
    }
}

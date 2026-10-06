package io.reqover.instrumentation;

import java.util.List;
import java.util.function.Supplier;

public class ReferenceTarget {
    public interface Lookup {
        String find(String key);
    }

    public enum Area {
        PARKING, WELFARE_CENTER
    }

    public Area area(Lookup lookup) {
        lookup.find("pub-1");
        return Area.PARKING;
    }

    public List<String> inLambda(Lookup lookup) {
        Supplier<String> deferred = () -> lookup.find("pub-2");
        return List.of(deferred.get());
    }

    public String areaName(Area area) {
        switch (area) {
            case PARKING:
                return "parking";
            default:
                return "other";
        }
    }
}

/*
 * Copyright 2026 Reqover contributors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.reqover.instrumentation;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReferenceTarget {
    public interface Lookup {
        String find(String key);

        default String findArea() {
            return Area.WELFARE_CENTER.name();
        }
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

    public Function<String, String> methodReference(Lookup lookup) {
        return lookup::find;
    }

    public String statusName(ReferenceStatus status) {
        switch (status) {
            case OPEN:
                return "open";
            default:
                return "closed";
        }
    }

    public int areaOrdinal(Area area) {
        return area.ordinal();
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

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

    public void setNameTrimmed(String name) {
        this.name = name.trim();
    }
}

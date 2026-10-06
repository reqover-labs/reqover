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

package io.reqover.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses {@code --key value} and {@code --flag} arguments.
 *
 * <p>Unknown options are rejected rather than ignored: a misspelled
 * {@code --fail-on-impact} in a CI job would otherwise turn a gate off
 * silently.
 */
final class CliArguments {
    private final Map<String, String> options;
    private final List<String> flags;

    private CliArguments(Map<String, String> options, List<String> flags) {
        this.options = options;
        this.flags = flags;
    }

    static CliArguments parse(String[] args, int from, List<String> knownOptions, List<String> knownFlags) {
        Map<String, String> options = new LinkedHashMap<>();
        List<String> flags = new ArrayList<>();

        for (int i = from; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new CliException("unexpected argument: " + arg);
            }
            String name = arg.substring(2);
            String inlineValue = null;
            int equals = name.indexOf('=');
            if (equals >= 0) {
                inlineValue = name.substring(equals + 1);
                name = name.substring(0, equals);
            }

            if (knownFlags.contains(name)) {
                if (inlineValue != null) {
                    throw new CliException("--" + name + " is a flag and takes no value");
                }
                flags.add(name);
                continue;
            }
            if (!knownOptions.contains(name)) {
                throw new CliException("unknown option: --" + name);
            }
            if (inlineValue != null) {
                options.put(name, inlineValue);
                continue;
            }
            if (i + 1 >= args.length) {
                throw new CliException("--" + name + " needs a value");
            }
            options.put(name, args[++i]);
        }

        return new CliArguments(options, flags);
    }

    String require(String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new CliException("--" + name + " is required");
        }
        return value;
    }

    String optional(String name, String fallback) {
        String value = options.get(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    boolean has(String name) {
        return options.containsKey(name);
    }

    boolean flag(String name) {
        return flags.contains(name);
    }
}

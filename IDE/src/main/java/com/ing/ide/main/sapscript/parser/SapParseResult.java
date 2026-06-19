/*
 * Copyright 2014 - 2025 ING Group
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
 */
package com.ing.ide.main.sapscript.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result object containing statistics and metadata from parsing a SAP script file.
 * Provides insights into parse quality and performance.
 */
public class SapParseResult {
    private final int objectsCreated;
    private final int actionsCreated;
    private final int linesProcessed;
    private final int linesParsed;
    private final long parseTimeMs;
    private final List<String> warnings;
    private final Map<String, Integer> actionTypeCounts;

    public SapParseResult(
        int objectsCreated,
        int actionsCreated,
        int linesProcessed,
        int linesParsed,
        long parseTimeMs,
        List<String> warnings,
        Map<String, Integer> actionTypeCounts
    ) {
        this.objectsCreated = objectsCreated;
        this.actionsCreated = actionsCreated;
        this.linesProcessed = linesProcessed;
        this.linesParsed = linesParsed;
        this.parseTimeMs = parseTimeMs;
        this.warnings = new ArrayList<>(warnings);
        this.actionTypeCounts = new HashMap<>(actionTypeCounts);
    }

    public int getObjectsCreated() {
        return objectsCreated;
    }

    public int getActionsCreated() {
        return actionsCreated;
    }

    public int getLinesProcessed() {
        return linesProcessed;
    }

    public int getLinesParsed() {
        return linesParsed;
    }

    public long getParseTimeMs() {
        return parseTimeMs;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public Map<String, Integer> getActionTypeCounts() {
        return Collections.unmodifiableMap(actionTypeCounts);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public double getParseEfficiency() {
        if (linesProcessed == 0) return 0.0;
        return (double) linesParsed / linesProcessed * 100.0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SAP Script Parse Results:\n");
        sb.append("  Lines processed: ").append(linesProcessed).append("\n");
        sb.append("  Lines with SAP actions: ").append(linesParsed);
        sb.append(" (").append(String.format("%.1f", getParseEfficiency())).append("%)\n");
        sb.append("  Objects created: ").append(objectsCreated).append("\n");
        sb.append("  Actions created: ").append(actionsCreated).append("\n");
        sb.append("  Parse time: ").append(parseTimeMs).append("ms\n");

        if (!actionTypeCounts.isEmpty()) {
            sb.append("  Action breakdown:\n");
            actionTypeCounts
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(
                    entry ->
                        sb
                            .append("    ")
                            .append(entry.getKey())
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n")
                );
        }

        if (hasWarnings()) {
            sb.append("  Warnings: ").append(warnings.size()).append("\n");
            warnings.forEach(w -> sb.append("    - ").append(w).append("\n"));
        }

        return sb.toString();
    }

    /**
     * Returns a summary suitable for logging or user display
     */
    public String getSummary() {
        return String.format(
            "Parsed %d lines (%d with SAP actions), created %d objects and %d test steps in %dms",
            linesProcessed,
            linesParsed,
            objectsCreated,
            actionsCreated,
            parseTimeMs
        );
    }
}

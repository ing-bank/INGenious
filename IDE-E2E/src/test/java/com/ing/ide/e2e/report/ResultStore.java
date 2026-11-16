package com.ing.ide.e2e.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stores test case results as JSON in {@code <run>/results/}, so reports can combine the results
 * of all test JVMs of one run.
 */
final class ResultStore {
    private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path dir;

    ResultStore(final Path runDir) {
        this.dir = runDir.resolve("results");
    }

    void save(final TestCaseResult result) throws IOException {
        Files.createDirectories(dir);
        MAPPER.writeValue(dir.resolve(result.identity().fileKey() + ".json").toFile(), result);
    }

    List<TestCaseResult> loadAll() throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        final List<TestCaseResult> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (final Path file : (Iterable<Path>) files.filter(
                f -> f.toString().endsWith(".json")
            )::iterator) {
                results.add(MAPPER.readValue(file.toFile(), TestCaseResult.class));
            }
        }
        results.sort(
            Comparator
                .comparing((TestCaseResult r) -> r.identity().scenarioCode())
                .thenComparing(r -> r.identity().testCaseCode())
        );
        return results;
    }
}

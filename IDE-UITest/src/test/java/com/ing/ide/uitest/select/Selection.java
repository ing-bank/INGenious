package com.ing.ide.uitest.select;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Which test cases to run, from {@code -Duitest.tests}, {@code -Duitest.scenarios},
 * {@code -Duitest.suite} and {@code -Duitest.exclude}.
 * <ul>
 *   <li>Values are comma-separated RTM codes; {@code *} and {@code ?} are wildcards; case is ignored.</li>
 *   <li>A test case runs if it matches any include; excludes are applied afterwards. Without any
 *   include, every test case is included.</li>
 *   <li>Suites are files {@code src/test/resources/suites/<name>.txt}, one entry per line: a test case
 *   or scenario code (pattern), {@code !pattern} to exclude, {@code @other} to include another suite,
 *   {@code #} for comments.</li>
 *   <li>An include that matches no test case, or an unknown suite, fails the run.</li>
 * </ul>
 */
public final class Selection {
    /** Test case codes to run. */
    public static final String TESTS = "uitest.tests";
    /** Scenario codes whose test cases to run. */
    public static final String SCENARIOS = "uitest.scenarios";
    /** Suite names to run. */
    public static final String SUITE = "uitest.suite";
    /** Test case or scenario codes to leave out. */
    public static final String EXCLUDE = "uitest.exclude";
    /** {@code true} to only print what would run. */
    public static final String LIST = "uitest.list";

    private static final String SUITE_DIR = "suites";
    private static final String SUITE_EXT = ".txt";

    private enum Target {
        TEST_CASE,
        SCENARIO,
        EITHER
    }

    private static final class Rule {
        private final boolean include;
        private final Target target;
        private final String text;
        private final Pattern pattern;
        private final String source;

        private Rule(
            final boolean include,
            final Target target,
            final String text,
            final String source
        ) {
            this.include = include;
            this.target = target;
            this.text = text;
            this.pattern = glob(text);
            this.source = source;
        }

        private boolean matches(final CatalogEntry entry) {
            final boolean testCase = pattern.matcher(entry.identity().testCaseCode()).matches();
            final boolean scenario = pattern.matcher(entry.identity().scenarioCode()).matches();
            switch (target) {
                case TEST_CASE:
                    return testCase;
                case SCENARIO:
                    return scenario;
                default:
                    return testCase || scenario;
            }
        }
    }

    private final List<Rule> rules;
    private final Path suiteDir;

    private Selection(final List<Rule> rules, final Path suiteDir) {
        this.rules = rules;
        this.suiteDir = suiteDir;
    }

    /**
     * Reads the selection from system properties; suites are read from the compiled test resources.
     *
     * @return the selection
     * @throws IllegalArgumentException if a suite does not exist or includes itself
     */
    public static Selection fromSystemProperties() {
        final Path suiteDir = TestCatalog.testClassesDir().resolve(SUITE_DIR);
        final List<Rule> rules = new ArrayList<>();
        values(TESTS).forEach(v -> rules.add(new Rule(true, Target.TEST_CASE, v, "-D" + TESTS)));
        values(SCENARIOS)
            .forEach(v -> rules.add(new Rule(true, Target.SCENARIO, v, "-D" + SCENARIOS)));
        for (final String suite : values(SUITE)) {
            readSuite(suiteDir, suite, rules, new ArrayDeque<>());
        }
        values(EXCLUDE).forEach(v -> rules.add(new Rule(false, Target.EITHER, v, "-D" + EXCLUDE)));
        return new Selection(rules, suiteDir);
    }

    /**
     * @return whether only the list of selected test cases should be printed
     */
    public static boolean listOnly() {
        return Boolean.parseBoolean(System.getProperty(LIST, "false").trim());
    }

    /**
     * @return whether nothing was selected explicitly (everything runs)
     */
    public boolean isEverything() {
        return rules.isEmpty();
    }

    /**
     * @param catalog all test cases
     * @return the keys ({@link CatalogEntry#key()}) of the selected test cases
     * @throws IllegalArgumentException if an include matches no test case
     */
    public Set<String> select(final List<CatalogEntry> catalog) {
        final List<Rule> includes = rules
            .stream()
            .filter(r -> r.include)
            .collect(Collectors.toList());
        for (final Rule rule : includes) {
            if (catalog.stream().noneMatch(rule::matches)) {
                throw new IllegalArgumentException(
                    "No UI test case matches '" +
                    rule.text +
                    "' (from " +
                    rule.source +
                    "). Known scenarios: " +
                    catalog
                        .stream()
                        .map(e -> e.identity().scenarioCode())
                        .distinct()
                        .collect(Collectors.joining(", ")) +
                    ". Run with -D" +
                    LIST +
                    "=true to see all test cases."
                );
            }
        }
        rules
            .stream()
            .filter(r -> !r.include && catalog.stream().noneMatch(r::matches))
            .forEach(
                r ->
                    System.err.println(
                        "[UI test] WARNING exclude '" +
                        r.text +
                        "' (from " +
                        r.source +
                        ") matches no test case"
                    )
            );
        return catalog
            .stream()
            .filter(e -> includes.isEmpty() || includes.stream().anyMatch(r -> r.matches(e)))
            .filter(e -> rules.stream().noneMatch(r -> !r.include && r.matches(e)))
            .map(CatalogEntry::key)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * @return a short description, e.g. {@code suite=smoke, exclude=GEN-SC001-TC005}
     */
    public String describe() {
        return Stream
            .of(TESTS, SCENARIOS, SUITE, EXCLUDE)
            .filter(p -> !values(p).isEmpty())
            .map(p -> p.substring("uitest.".length()) + "=" + String.join(",", values(p)))
            .collect(Collectors.joining(", "));
    }

    private static void readSuite(
        final Path suiteDir,
        final String name,
        final List<Rule> rules,
        final Deque<String> chain
    ) {
        if (chain.contains(name)) {
            throw new IllegalArgumentException(
                "Suite '" +
                name +
                "' includes itself: " +
                String.join(" -> ", chain) +
                " -> " +
                name
            );
        }
        final Path file = suiteDir.resolve(name + SUITE_EXT);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                "Unknown suite '" + name + "'. Available: " + String.join(", ", available(suiteDir))
            );
        }
        chain.push(name);
        final List<String> lines = readLines(file);
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i).replaceAll("#.*$", "").trim();
            final String source = "suite " + name + ", line " + (i + 1);
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("@")) {
                readSuite(suiteDir, line.substring(1).trim(), rules, chain);
            } else if (line.startsWith("!")) {
                rules.add(new Rule(false, Target.EITHER, line.substring(1).trim(), source));
            } else {
                rules.add(new Rule(true, Target.EITHER, line, source));
            }
        }
        chain.pop();
    }

    private static List<String> available(final Path suiteDir) {
        if (!Files.isDirectory(suiteDir)) {
            return List.of("(none)");
        }
        try (Stream<Path> files = Files.list(suiteDir)) {
            return files
                .map(f -> f.getFileName().toString())
                .filter(n -> n.endsWith(SUITE_EXT))
                .map(n -> n.substring(0, n.length() - SUITE_EXT.length()))
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of("(unreadable)");
        }
    }

    private static List<String> readLines(final Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read suite " + file, e);
        }
    }

    private static List<String> values(final String property) {
        return Arrays
            .stream(System.getProperty(property, "").split(","))
            .map(String::trim)
            .filter(v -> !v.isEmpty())
            .collect(Collectors.toList());
    }

    private static Pattern glob(final String text) {
        final StringBuilder regex = new StringBuilder();
        for (final char c : text.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public String toString() {
        return rules.isEmpty() ? "all test cases" : describe().toLowerCase(Locale.ROOT);
    }
}

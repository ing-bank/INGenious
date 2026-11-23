package com.ing.ide.uitest.select;

import com.ing.ide.uitest.report.TestIdentity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;

/**
 * Keeps only the test methods selected by {@link Selection}, before TestNG runs anything. A class
 * left without methods does not run its {@code @BeforeClass}, so its IDE is never started.
 * Filtered-out test cases are not reported at all (not even as skipped). A selected test case's
 * {@code dependsOnMethods} are kept as well.
 */
public final class TestSelectionInterceptor implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(
        final List<IMethodInstance> methods,
        final ITestContext context
    ) {
        final boolean listOnly = Selection.listOnly();
        final Selection selection = Selection.fromSystemProperties();
        if (selection.isEverything() && !listOnly) {
            return methods;
        }
        final Set<String> selected = selection.select(TestCatalog.load());
        addDependencies(methods, selected);
        final List<IMethodInstance> kept = methods
            .stream()
            .filter(m -> selected.contains(key(m.getMethod())))
            .collect(Collectors.toList());
        if (listOnly) {
            kept.forEach(m -> System.out.println("[UI test] would run " + describe(m.getMethod())));
            return List.of();
        }
        if (!methods.isEmpty()) {
            System.out.println(
                "[UI test] " +
                methods.get(0).getMethod().getRealClass().getSimpleName() +
                ": running " +
                kept.size() +
                " of " +
                methods.size() +
                " test case(s) (" +
                selection.describe() +
                ")"
            );
        }
        return kept;
    }

    // Adds methods the selected ones depend on (dependsOnMethods), transitively
    private static void addDependencies(
        final List<IMethodInstance> methods,
        final Set<String> selected
    ) {
        boolean added = true;
        while (added) {
            added = false;
            for (final IMethodInstance m : methods) {
                if (!selected.contains(key(m.getMethod()))) {
                    continue;
                }
                for (final String dependency : m.getMethod().getMethodsDependedUpon()) {
                    final int dot = dependency.lastIndexOf('.');
                    final String dependencyKey =
                        dependency.substring(0, dot) + "#" + dependency.substring(dot + 1);
                    added |= selected.add(dependencyKey);
                }
            }
        }
    }

    private static String key(final ITestNGMethod method) {
        return CatalogEntry.key(method.getRealClass(), method.getMethodName());
    }

    private static String describe(final ITestNGMethod method) {
        final TestIdentity id = TestIdentity.of(
            method.getRealClass(),
            method.getConstructorOrMethod().getMethod()
        );
        return String.format(
            "%-16s %-10s %s  [%s#%s]",
            id.testCaseCode(),
            id.scenarioCode(),
            id.testCaseDescription(),
            method.getRealClass().getSimpleName(),
            method.getMethodName()
        );
    }
}

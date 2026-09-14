package com.ing.datalib.component.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ing.datalib.component.Project;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/** Tests for {@link ProjectMigrationRegistry} ordering, idempotency and failure isolation. */
public class ProjectMigrationRegistryTest {
    private final List<String> ranIds = new ArrayList<>();

    @AfterMethod
    public void tearDown() {
        ProjectMigrationRegistry.unregister("second");
        ProjectMigrationRegistry.unregister("first");
        ProjectMigrationRegistry.unregister("skipped");
        ProjectMigrationRegistry.unregister("boom");
        ProjectMigrationRegistry.unregister("after-boom");
        ranIds.clear();
    }

    private ProjectMigration unit(String id, int order, boolean applies) {
        return unit(id, order, applies, false);
    }

    private ProjectMigration unit(String id, int order, boolean applies, boolean throwsOnMigrate) {
        return new ProjectMigration() {

            @Override
            public String id() {
                return id;
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public boolean appliesTo(Project project) {
                return applies;
            }

            @Override
            public void migrate(Project project) throws Exception {
                if (throwsOnMigrate) {
                    throw new RuntimeException("boom");
                }
                ranIds.add(id);
            }
        };
    }

    @Test
    public void runAll_runsOnlyApplicableUnitsInOrder() {
        ProjectMigrationRegistry.register(unit("second", 200, true));
        ProjectMigrationRegistry.register(unit("first", 100, true));
        ProjectMigrationRegistry.register(unit("skipped", 50, false));

        ProjectMigrationRegistry.runAll(mock(Project.class));

        assertThat(ranIds).containsExactly("first", "second");
    }

    @Test
    public void aThrowingUnitIsSkippedWithoutAbortingTheRest() {
        ProjectMigrationRegistry.register(unit("boom", 10, true, true));
        ProjectMigrationRegistry.register(unit("after-boom", 20, true));

        ProjectMigrationRegistry.runAll(mock(Project.class));

        assertThat(ranIds).containsExactly("after-boom");
    }

    @Test
    public void registeringTheSameIdTwiceReplacesTheEarlierUnit() {
        ProjectMigrationRegistry.register(unit("first", 100, true));
        ProjectMigrationRegistry.register(unit("first", 100, false));

        ProjectMigrationRegistry.runAll(mock(Project.class));

        assertThat(ranIds).isEmpty();
    }

    @Test
    public void unregisterRemovesTheUnit() {
        ProjectMigrationRegistry.register(unit("first", 100, true));
        ProjectMigrationRegistry.unregister("first");

        ProjectMigrationRegistry.runAll(mock(Project.class));

        assertThat(ranIds).isEmpty();
    }
}

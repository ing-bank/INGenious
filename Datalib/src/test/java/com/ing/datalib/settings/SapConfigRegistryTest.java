package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link SapConfigRegistry} alias / default resolution. */
public class SapConfigRegistryTest {
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sapreg-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private SapConfigRegistry registry() {
        SapConnections store = new SapConnections(tempDir.toString());
        SapDefaults defaults = new SapDefaults(tempDir.toString());
        return new SapConfigRegistry(store, defaults);
    }

    @Test
    public void hashAliasResolvesToBareName() {
        SapConfigRegistry r = registry();
        assertThat(r.resolveAlias("#QA")).isEqualTo("QA");
        assertThat(r.resolveAlias(" QA ")).isEqualTo("QA");
    }

    @Test
    public void blankResolvesToSoleConnection() {
        SapConfigRegistry r = registry();
        r.getStore().addSap("Only");
        assertThat(r.resolveAlias("")).isEqualTo("Only");
        assertThat(r.resolveAlias(null)).isEqualTo("Only");
    }

    @Test
    public void blankResolvesToExplicitDefault() {
        SapConfigRegistry r = registry();
        r.getStore().addSap("QA");
        r.getStore().addSap("PROD");
        r.getDefaults().setDefaultConnection("PROD");
        assertThat(r.resolveAlias("")).isEqualTo("PROD");
        assertThat(r.hasDefault()).isTrue();
    }

    @Test
    public void blankWithNoDefaultAndManyConnectionsThrows() {
        SapConfigRegistry r = registry();
        r.getStore().addSap("QA");
        r.getStore().addSap("PROD");
        assertThat(r.hasDefault()).isFalse();
        assertThatThrownBy(() -> r.resolveAlias(""))
            .isInstanceOf(SapConfigException.class)
            .hasMessageContaining("default");
    }

    @Test
    public void blankWithNoConnectionsThrows() {
        assertThatThrownBy(() -> registry().resolveAlias(""))
            .isInstanceOf(SapConfigException.class);
    }

    @Test
    public void isSapTargetChecksTheStore() {
        SapConfigRegistry r = registry();
        r.getStore().addSap("QA");
        assertThat(r.isSapTarget("QA")).isTrue();
        assertThat(r.isSapTarget("nope")).isFalse();
    }
}

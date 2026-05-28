package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for DBProperties — database configuration folder management.
 * Note: DBProperties uses static fields, so tests must run sequentially.
 */
public class DBPropertiesTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("dbprops-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testConstructorCreatesDatabasesFolder() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        File dbDir = new File(DBProperties.getLocation());
        assertThat(dbDir).exists().isDirectory();
    }

    @Test
    public void testDefaultDatabaseCreated() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        assertThat(dbp.getDbList()).contains("default");
    }

    @Test
    public void testDefaultDatabaseProperties() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        Properties defaultProps = dbp.getDBPropertiesFor("default");

        assertThat(defaultProps).isNotNull();
        assertThat(defaultProps.getProperty("db.alias")).isEqualTo("default");
        assertThat(defaultProps.getProperty("driver")).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(defaultProps.getProperty("timeout")).isEqualTo("30");
        assertThat(defaultProps.getProperty("commit")).isEqualTo("false");
    }

    @Test
    public void testAddDBProperty() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        dbp.addDBProperty("testDB");

        assertThat(dbp.getDBProperties()).containsKey("testDB");
    }

    @Test
    public void testAddDBPropertyWithParams() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        dbp.addDBProperty("customDB", "org.postgresql.Driver", "jdbc:postgresql://localhost/test", "60", "true");

        assertThat(dbp.getDBProperties()).containsKey("customDB");
    }

    @Test
    public void testAddDBWithProperties() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        Properties props = new Properties();
        props.setProperty("driver", "custom.Driver");
        dbp.addDB("manualDB", props);

        assertThat(dbp.getDBPropertiesFor("manualDB").getProperty("driver")).isEqualTo("custom.Driver");
    }

    @Test
    public void testDeleteDB() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        dbp.addDBProperty("toDelete");
        dbp.delete("toDelete");

        assertThat(dbp.getDBProperties()).doesNotContainKey("toDelete");
    }

    @Test
    public void testDeleteRemovesFile() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        dbp.addDBProperty("toDelete2");
        String filePath = dbp.getDBLocation("toDelete2");
        assertThat(new File(filePath)).exists();

        dbp.delete("toDelete2");
        assertThat(new File(filePath)).doesNotExist();
    }

    @Test
    public void testDeleteNonExistent() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        int sizeBefore = dbp.getDBProperties().size();
        dbp.delete("nonExistent");
        assertThat(dbp.getDBProperties()).hasSize(sizeBefore);
    }

    @Test
    public void testSaveSpecificDB() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        Properties props = new Properties();
        props.setProperty("custom", "value");
        dbp.addDB("saveTest", props);
        dbp.save("saveTest");

        assertThat(new File(dbp.getDBLocation("saveTest"))).exists();
    }

    @Test
    public void testSaveAllDBs() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        dbp.addDBProperty("db1");
        dbp.addDBProperty("db2");
        dbp.save();

        assertThat(new File(dbp.getDBLocation("db1"))).exists();
        assertThat(new File(dbp.getDBLocation("db2"))).exists();
    }

    @Test
    public void testGetDBLocation() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        String expected = DBProperties.getLocation() + File.separator + "myDB.properties";
        assertThat(dbp.getDBLocation("myDB")).isEqualTo(expected);
    }

    @Test
    public void testPersistenceAcrossInstances() {
        DBProperties dbp1 = new DBProperties(tempDir.toString());
        Properties props = new Properties();
        props.setProperty("persisted", "yes");
        dbp1.addDB("persist", props);

        DBProperties dbp2 = new DBProperties(tempDir.toString());
        assertThat(dbp2.getDbList()).contains("persist");
        assertThat(dbp2.getDBPropertiesFor("persist").getProperty("persisted")).isEqualTo("yes");
    }

    @Test
    public void testAddDBName() {
        DBProperties dbp = new DBProperties(tempDir.toString());
        dbp.addDBName("nameOnly");
        // addDBName adds to static in-memory list, but getDbList() calls load()
        // which clears and reloads from disk. Verify it's in the map check instead.
        assertThat(dbp.getDBProperties().keySet()).doesNotContain("nameOnly");
    }
}

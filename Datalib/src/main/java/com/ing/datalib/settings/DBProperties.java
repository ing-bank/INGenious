package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class DBProperties {

    private final Map<String, LinkedProperties> dbProperties = new HashMap<>();

    private static String location;
    private static ArrayList<String> dbList = new ArrayList<>();

    public DBProperties(String location) {
        this.location = location;
        createDBFolder();
        load();
    }

    public Map<String, LinkedProperties> getDBProperties() {
        return dbProperties;
    }

    public ArrayList<String> getDbList() {
        load();
        return dbList;
    }

    public LinkedProperties getDBPropertiesFor(String dbName) {
        return dbProperties.get(dbName);
    }

    private void load() {
        dbList.clear();
        File dbFile = new File(getLocation());
        if (dbFile.exists()) {
            for (File dbfile : dbFile.listFiles()) {
                if (dbfile.getName().endsWith(".properties")) {
                    String dbAlias = dbfile.getName().replace(".properties", "");
                    if (!dbList.contains(dbAlias)) {
                        dbList.add(dbAlias);
                        dbProperties.put(dbfile.getName().replace(".properties", ""), PropUtils.load(dbfile));
                    }
                }
            }
        }
    }

    public void addDBName(String dbName) {
        dbList.add(dbName);
    }


    public void addDB(String dbName, LinkedProperties props) {
        dbProperties.put(dbName, props);
    }

    public void addDBProperty(String dbName) {
        addDBProperty(dbName, "com.mysql.cj.jdbc.Driver", "jdbc:<Database>://<Host>:<Port>/<Database name>", "30", "false");
    }

    public void addDBProperty(String dbName, String driver, String connectionString, String timeout, String commit) {
        LinkedProperties x = new LinkedProperties();
        x.setProperty("db.alias", dbName);
        x.setProperty("user", "");
        x.setProperty("password", "");
        x.setProperty("driver", driver);
        x.setProperty("connectionString", connectionString);
        x.setProperty("timeout", timeout);
        x.setProperty("commit", commit);
        addDB(dbName, x);

    }


    public void save() {
       // createDBFolder();
        for (Map.Entry<String, LinkedProperties> entry : dbProperties.entrySet()) {
            String dbName = entry.getKey();
            Properties dbProp = entry.getValue();
            PropUtils.save(dbProp, getDBLocation(dbName));
        }
    }

    public void save(String dbName) {
       // createDBFolder();
        if (dbProperties.containsKey(dbName)) {
            PropUtils.save(dbProperties.get(dbName), getDBLocation(dbName));
        }
    }

    public void delete(String dbName) {
        if (dbProperties.containsKey(dbName)) {
            File db = new File(getDBLocation(dbName));
            if (db.exists()) {
                db.delete();
            }
            dbProperties.remove(dbName);
            dbList.remove(dbName);
        }
    }

    public String getDBLocation(String dbName) {
        return getLocation() + File.separator + dbName + ".properties";
    }

    public static String getLocation() {
        return location + File.separator + "Databases";
    }

    public void setLocation(String location) {
        this.location = location;
    }

    private void createDBFolder() {
        File dbs = new File(getLocation());
        if (!dbs.exists()) {
            dbs.mkdirs();
            createDefaultFile(getLocation());
        }
    }

    private void createDefaultFile(String location) {
        String fileName = location + File.separator + "default.properties";
        File propertiesFile = new File(fileName);
        if (!propertiesFile.exists()) {
            try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
                Properties prop = new Properties();
                // Add default key-value pairs
                prop.setProperty("db.alias", "default");
                prop.setProperty("user", "");
                prop.setProperty("password", "");
                prop.setProperty("driver", "com.mysql.cj.jdbc.Driver");
                prop.setProperty("connectionString", "jdbc:<Database>://<Host>:<Port>/<Database name>");
                prop.setProperty("timeout", "30");
                prop.setProperty("commit", "false");
                prop.store(fos, null);
            } catch (IOException e) {
                System.err.println("Error writing to default.properties file: " + e.getMessage());
            }
        } else {
            System.out.println("default.properties file already exists: " + location);
        }
    }


}
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

    private final Map<String, Properties> dbProperties = new HashMap<>();

    private static String location;
    private static ArrayList<String> dbList = new ArrayList<>();

    public DBProperties(String location) {
        this.location = location;
        createDBFolder();
        load();
    }

    public Map<String, Properties> getDBProperties() {
        return dbProperties;
    }

    public ArrayList<String> getDbList() {
        load();
        return dbList;
    }

    public Properties getDBPropertiesFor(String dbName) {
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


    public void addDB(String dbName, Properties prop) {
        dbProperties.put(dbName, prop);
        save();
    }

    public void addDBProperty(String dbName) {
        addDBProperty(dbName, "com.mysql.cj.jdbc.Driver", "jdbc:<Database>://<Host>:<Port>/<Database name>", "30", "false");
    }

    public void addDBProperty(String dbName, String driver, String connectionString, String timeout, String commit) {
        Properties prop = new Properties();
        prop = setDatabaseProperties(prop);
        addDB(dbName, prop);

    }


    public void save() {
       // createDBFolder();
        for (Map.Entry<String, Properties> entry : dbProperties.entrySet()) {
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
                prop = setDatabaseProperties(prop);
                prop.store(fos, null);
            } catch (IOException e) {
                System.err.println("Error writing to default.properties file: " + e.getMessage());
            }
        } else {
            System.out.println("default.properties file already exists: " + location);
        }
    }
    
    private Properties setDatabaseProperties(Properties prop) {
        prop.setProperty("db.alias", "default");
        prop.setProperty("user", "");
        prop.setProperty("password", "");
        prop.setProperty("driver", "com.mysql.cj.jdbc.Driver");
        prop.setProperty("connectionString", "jdbc:<Database>://<Host>:<Port>/<Database name>");
        prop.setProperty("timeout", "30");
        prop.setProperty("commit", "false");
        return prop;

    }


}
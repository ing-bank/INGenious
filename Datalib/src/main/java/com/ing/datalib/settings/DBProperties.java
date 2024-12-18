/*
 * Copyright 2014 - 2017 Cognizant Technology Solutions
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
    public static ArrayList<String> dbList = new ArrayList<>();

    public DBProperties(String location) {
        this.location = location;
        load();
    }

    public Map<String, LinkedProperties> getDBProperties() {
        return dbProperties;
    }

    public ArrayList<String> getDbList() {
        load();
        if(!dbList.contains("default"))
            dbList.add("default");
        return dbList;
    }

    public LinkedProperties getDBPropertiesFor(String dbName) {
        return dbProperties.get(dbName);
    }

    private void load() {
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

    public static ArrayList<String> getValuesAsList() {
        File folder = new File(getLocation().toString());
        File[] listOfDBs = folder.listFiles();
        if (listOfDBs != null) {
            for (File file : listOfDBs) {
                if (file.isFile()) {
                    dbList.add(file.getName());
                }
            }
        }
        return dbList;
    }

    public void addDB(String dbName, LinkedProperties props) {
        dbProperties.put(dbName, props);
    }

    public void addDBProperty(String dbName) {
        addDBProperty(dbName, "<userID>", "<password>", "com.mysql.cj.jdbc.Driver", "jdbc:<Database>://<Host>:<Port>/<Database name>", "30", "false");
    }

    public void addDBProperty(String dbName, String user, String password, String driver, String connectionString, String timeout, String commit) {
        LinkedProperties x = new LinkedProperties();
        x.setProperty("db.alias", dbName);
        x.setProperty("user", user);
        x.setProperty("password", password);
        x.setProperty("driver", driver);
        x.setProperty("connectionString", connectionString);
        x.setProperty("timeout", timeout);
        x.setProperty("commit", commit);
        addDB(dbName, x);

    }


    public void save() {
        createDBFolder();
        for (Map.Entry<String, LinkedProperties> entry : dbProperties.entrySet()) {
            String dbName = entry.getKey();
            Properties dbProp = entry.getValue();
            PropUtils.save(dbProp, getDBLocation(dbName));
        }
    }

    public void save(String dbName) {
        createDBFolder();
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
                prop.setProperty("user", "<username>");
                prop.setProperty("password", "<password>");
                prop.setProperty("driver", "com.mysql.cj.jdbc.Driver");
                prop.setProperty("connectionString", "jdbc:<Database>://<Host>:<Port>/<Database name>");
                prop.setProperty("timeout", "30");
                prop.setProperty("commit", "false");
                // Write properties to the file
                prop.store(fos, "Default Properties");
                System.out.println("default.properties file created: " + location);
            } catch (IOException e) {
                System.err.println("Error writing to default.properties file: " + e.getMessage());
            }
        } else {
            System.out.println("default.properties file already exists: " + location);
        }
    }


}

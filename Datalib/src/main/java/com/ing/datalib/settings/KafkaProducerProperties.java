package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Manages named Kafka <b>producer</b> configurations stored as per-alias
 * {@code .properties} files under {@code <Settings>/Kafka/Producers}.
 * <p>
 * Modelled on {@link DriverProperties} (API configs): each configuration is
 * identified by a unique name and referenced from a test step via
 * {@code #<name>} in the Condition column. A {@code default} configuration is
 * created automatically when the folder is first initialised.
 */
public class KafkaProducerProperties extends LinkedProperties {
    private static String location;
    private final ArrayList<String> producerList = new ArrayList<>();
    private final Map<String, Properties> producerPropMap = new HashMap<>();
    private String currLoadedProducerConfig;

    public KafkaProducerProperties(String location) {
        KafkaProducerProperties.location = location;
        createFolder();
        load();
        currLoadedProducerConfig = "default";
    }

    /** Directory holding the producer {@code .properties} files. */
    public static String getLocation() {
        return location + File.separator + "Kafka" + File.separator + "Producers";
    }

    public void setLocation(String location) {
        KafkaProducerProperties.location = location;
    }

    public String getProducerLocation(String name) {
        return getLocation() + File.separator + name + ".properties";
    }

    private void createFolder() {
        File folder = new File(getLocation());
        if (!folder.exists()) {
            folder.mkdirs();
            Properties prop = defaultProducerProperties("default");
            PropUtils.save(prop, getProducerLocation("default"));
        }
    }

    private void load() {
        producerList.clear();
        producerPropMap.clear();
        File folder = new File(getLocation());
        if (!folder.exists()) return;
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".properties"));
        if (files == null) return;
        for (File file : files) {
            String name = file.getName().replace(".properties", "");
            if (!producerList.contains(name)) {
                producerList.add(name);
                producerPropMap.put(name, PropUtils.load(file));
            }
        }
    }

    /** Default keys seeded on first run and by the "New" button. */
    private Properties defaultProducerProperties(String name) {
        Properties prop = new LinkedProperties();
        prop.setProperty("producer.alias", name);
        prop.setProperty("bootstrap.servers", "");
        prop.setProperty("producer.topic", "");
        prop.setProperty(
            "key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer"
        );
        prop.setProperty(
            "value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer"
        );
        prop.setProperty("partition", "");
        prop.setProperty("schema.registry.url", "");
        prop.setProperty("auto.register.schemas", "false");
        prop.setProperty("shared.secret", "");
        prop.setProperty("ssl.enabled", "false");
        prop.setProperty("ssl.keystore.location", "");
        prop.setProperty("ssl.keystore.password", "");
        prop.setProperty("ssl.key.password", "");
        return prop;
    }

    public ArrayList<String> getProducerList() {
        load();
        return producerList;
    }

    public Properties getProducerPropertiesFor(String name) {
        return producerPropMap.get(name);
    }

    public boolean doesProducerConfigExist(String name) {
        return producerList.contains(name);
    }

    public String getCurrLoadedProducerConfig() {
        return currLoadedProducerConfig;
    }

    public void setCurrLoadedProducerConfig(String name) {
        this.currLoadedProducerConfig = name;
    }

    /** Creates a new producer configuration seeded with default keys and persists it. */
    public void addProducer(String name) {
        addProducer(name, defaultProducerProperties(name));
    }

    public void addProducer(String name, Properties prop) {
        if (!producerList.contains(name)) {
            producerList.add(name);
        }
        producerPropMap.put(name, prop);
        save(name);
    }

    public void save() {
        for (Map.Entry<String, Properties> entry : producerPropMap.entrySet()) {
            PropUtils.save(entry.getValue(), getProducerLocation(entry.getKey()));
        }
    }

    public void save(String name) {
        if (producerPropMap.containsKey(name)) {
            PropUtils.save(producerPropMap.get(name), getProducerLocation(name));
        }
    }

    public void delete(String name) {
        if (producerPropMap.containsKey(name)) {
            File file = new File(getProducerLocation(name));
            if (file.exists()) {
                file.delete();
            }
            producerPropMap.remove(name);
            producerList.remove(name);
        }
    }
}

package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Manages named Kafka <b>consumer</b> configurations stored as per-alias
 * {@code .properties} files under {@code <Settings>/Kafka/Consumers}.
 * <p>
 * Modelled on {@link DriverProperties} (API configs): each configuration is
 * identified by a unique name and referenced from a test step via
 * {@code #<name>} in the Condition column. A {@code default} configuration is
 * created automatically when the folder is first initialised.
 */
public class KafkaConsumerProperties extends LinkedProperties {
    private static String location;
    private final ArrayList<String> consumerList = new ArrayList<>();
    private final Map<String, Properties> consumerPropMap = new HashMap<>();
    private String currLoadedConsumerConfig;

    public KafkaConsumerProperties(String location) {
        KafkaConsumerProperties.location = location;
        createFolder();
        load();
        currLoadedConsumerConfig = "default";
    }

    /** Directory holding the consumer {@code .properties} files. */
    public static String getLocation() {
        return location + File.separator + "Kafka" + File.separator + "Consumers";
    }

    public void setLocation(String location) {
        KafkaConsumerProperties.location = location;
    }

    public String getConsumerLocation(String name) {
        return getLocation() + File.separator + name + ".properties";
    }

    private void createFolder() {
        File folder = new File(getLocation());
        if (!folder.exists()) {
            folder.mkdirs();
            Properties prop = defaultConsumerProperties("default");
            PropUtils.save(prop, getConsumerLocation("default"));
        }
    }

    private void load() {
        consumerList.clear();
        consumerPropMap.clear();
        File folder = new File(getLocation());
        if (!folder.exists()) return;
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".properties"));
        if (files == null) return;
        for (File file : files) {
            String name = file.getName().replace(".properties", "");
            if (!consumerList.contains(name)) {
                consumerList.add(name);
                consumerPropMap.put(name, PropUtils.load(file));
            }
        }
    }

    /** Default keys seeded on first run and by the "New" button. */
    private Properties defaultConsumerProperties(String name) {
        Properties prop = new LinkedProperties();
        prop.setProperty("consumer.alias", name);
        prop.setProperty("bootstrap.servers", "");
        prop.setProperty("consumer.topic", "");
        prop.setProperty("group.id", "");
        prop.setProperty(
            "value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer"
        );
        prop.setProperty("poll.retries", "5");
        prop.setProperty("poll.interval.ms", "1000");
        prop.setProperty("max.poll.records", "500");
        prop.setProperty("schema.registry.url", "");
        prop.setProperty("ssl.enabled", "false");
        prop.setProperty("ssl.keystore.location", "");
        prop.setProperty("ssl.keystore.password", "");
        prop.setProperty("ssl.key.password", "");
        return prop;
    }

    public ArrayList<String> getConsumerList() {
        load();
        return consumerList;
    }

    public Properties getConsumerPropertiesFor(String name) {
        return consumerPropMap.get(name);
    }

    public boolean doesConsumerConfigExist(String name) {
        return consumerList.contains(name);
    }

    public String getCurrLoadedConsumerConfig() {
        return currLoadedConsumerConfig;
    }

    public void setCurrLoadedConsumerConfig(String name) {
        this.currLoadedConsumerConfig = name;
    }

    /** Creates a new consumer configuration seeded with default keys and persists it. */
    public void addConsumer(String name) {
        addConsumer(name, defaultConsumerProperties(name));
    }

    public void addConsumer(String name, Properties prop) {
        if (!consumerList.contains(name)) {
            consumerList.add(name);
        }
        consumerPropMap.put(name, prop);
        save(name);
    }

    public void save() {
        for (Map.Entry<String, Properties> entry : consumerPropMap.entrySet()) {
            PropUtils.save(entry.getValue(), getConsumerLocation(entry.getKey()));
        }
    }

    public void save(String name) {
        if (consumerPropMap.containsKey(name)) {
            PropUtils.save(consumerPropMap.get(name), getConsumerLocation(name));
        }
    }

    public void delete(String name) {
        if (consumerPropMap.containsKey(name)) {
            File file = new File(getConsumerLocation(name));
            if (file.exists()) {
                file.delete();
            }
            consumerPropMap.remove(name);
            consumerList.remove(name);
        }
    }
}

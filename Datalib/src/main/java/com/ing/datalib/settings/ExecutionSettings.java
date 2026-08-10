package com.ing.datalib.settings;

/**
 *
 *
 */
public class ExecutionSettings {
    private final RunSettings runSettings;

    private final TestMgmtSettings testMgmgtSettings;

    private final KafkaSSLConfigurations kafkaSSLConfigurations;

    public ExecutionSettings(String location) {
        runSettings = new RunSettings(location);
        testMgmgtSettings = new TestMgmtSettings(location);
        kafkaSSLConfigurations = new KafkaSSLConfigurations(location);
    }

    public RunSettings getRunSettings() {
        return runSettings;
    }

    public TestMgmtSettings getTestMgmgtSettings() {
        return testMgmgtSettings;
    }

    public KafkaSSLConfigurations getKafkasslConfiguration() {
        return kafkaSSLConfigurations;
    }

    public void setLocation(String location) {
        runSettings.setLocation(location);
        testMgmgtSettings.setLocation(location);
        kafkaSSLConfigurations.setLocation(location);
    }

    public void save() {
        runSettings.save();
        testMgmgtSettings.save();
        kafkaSSLConfigurations.save();
    }
}

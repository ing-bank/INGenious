package com.ing.datalib.settings;

public class KafkaSSLConfigurations extends AbstractPropSettings {

    public KafkaSSLConfigurations(String location) {
        super(location, "KafkaSSLConfigurations");
        if (isEmpty()) {
            loadDefault();
        }
    }

    private void loadDefault() {
        put("Producer_ssl_Enabled", "false");
        // put("Producer_Truststore_Location", "");
        // put("Producer_Truststore_Password", "");
        put("Producer_Keystore_Location", "");
        put("Producer_Keystore_Password", "");
        put("Producer_Key_Password", "");

        put("Consumer_ssl_Enabled", "false");
        // put("Consumer_Truststore_Location", "");
        // put("Consumer_Truststore_Password", "");
        put("Consumer_Keystore_Location", "");
        put("Consumer_Keystore_Password", "");
        put("Consumer_Key_Password", "");
    }
}

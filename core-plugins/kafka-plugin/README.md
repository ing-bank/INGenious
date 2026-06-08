# Kafka Plugin for INGenious Framework

This plugin provides comprehensive Kafka producer and consumer operations for the INGenious test automation framework, including support for Avro schemas, SSL/TLS configuration, and message assertions using JSONPath and XPath.

## Features

- **Kafka Producer Operations**
  - Support for String and Avro serialization
  - Custom headers, partitions, timestamps, and keys
  - Schema Registry integration with auto-registration
  - SSL/TLS support

- **Kafka Consumer Operations**
  - Configurable polling with retries
  - Support for String and Avro deserialization
  - Message filtering using JSONPath/XPath conditions
  - Consumer group management

- **Message Validation & Storage**
  - Assert JSON/XML content using JSONPath/XPath
  - Store consumed values in test data sheets
  - Support for multiple assertion conditions

## Installation

### Automatic Deployment (Recommended)

- Navigate to core-plugins/kafka-plugin/pom.xml and update lines 141 and 142 with your INGenious path, then save
  ```bash
  <property name="deploy.dir" value="{Insert directory path of INGenious}"/>
  <property name="framework.lib.dir" value="{Insert directory path of INGenious}/lib"/>
  ```
- Then run the following commands:
  ```bash
  cd core-plugins/kafka-plugin
  mvn clean install
  ```

This automatically:
- ✅ Copies `kafka-plugin.jar` to `/plugins/kafka-plugin/`
- ✅ Copies all 31 Kafka dependencies to `/lib/` (main framework lib)
- ✅ Makes dependencies available to the framework classloader

**IMPORTANT**: Kafka dependencies **must** be in the main framework `lib` folder, not just the plugin's `lib` subfolder. The automated build handles this correctly.

### Manual Deployment (If Needed)

If you need to deploy manually to a different INGenious installation:

```bash
# Copy plugin JAR
cp target/kafka-plugin-1.0.jar <INGENIOUS_HOME>/plugins/kafka-plugin/kafka-plugin.jar

# Copy ALL dependencies to main framework lib (REQUIRED for classloader)
cp target/lib/*.jar <INGENIOUS_HOME>/lib/
```

### Restart INGenious IDE

The plugin will be automatically loaded on the next INGenious IDE startup.

## Dependencies

The plugin includes the following dependencies:
- Apache Kafka Clients 3.6.0
- Apache Avro 1.11.3
- Confluent Kafka Avro Serializer 7.5.1
- Jackson Databind 2.15.3
- JsonPath 2.8.0

## Notes

- Each Kafka object instance uses the Object Name as its context key
- Producer and Consumer operations maintain separate state per object instance
- SSL/TLS configuration methods are provided but require additional setup
- The plugin supports datasheet variables (`{SheetName:Column}`) and user-defined variables in message payloads
- Multiple identification conditions can be specified for message filtering; all conditions must match

## Troubleshooting

### Plugin Not Loading
- Ensure the JAR and all dependencies are in the correct plugins folder
- Check INGenious logs for plugin loading errors
- Verify the manifest entry `pluginEntryClasses` is set to `com.ing.plugin.kafka.KafkaPlugin`

### Connection Issues
- Verify Kafka broker is running and accessible
- Check bootstrap server addresses and ports
- For SSL connections, ensure certificates and configurations are properly set

### Serialization Errors
- For Avro messages, ensure the schema is loaded before producing messages
- Verify Schema Registry is accessible if using Avro serialization
- Check that the message payload matches the Avro schema structure

### Consumer Not Finding Messages
- Verify the topic has messages
- Check that consumer group ID is unique or reset offsets if needed
- Ensure identification conditions (JSONPath/XPath) are correct
- Increase poll retries and interval if messages are produced slowly

## Version History

### 1.0
- Initial release with full Kafka producer and consumer support
- Avro serialization/deserialization
- JSONPath and XPath assertion support
- SSL/TLS configuration support
- Message filtering and data storage capabilities

## License

This plugin is part of the INGenious Framework.

## Support

For issues or questions, please contact the INGenious support team or create an issue in the project repository.

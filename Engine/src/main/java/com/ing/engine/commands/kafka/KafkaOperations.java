package com.ing.engine.commands.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Kafka producer/consumer commands.
 *
 * <p>Connection, serialization and SSL settings are read from <b>named
 * configurations</b> defined in the Settings panel (Kafka Configurations tab)
 * and referenced from a step via {@code #<configName>} in the Condition column
 * &mdash; mirroring the API {@code #alias} pattern. The legacy per-step
 * {@code setXxx} actions are retained as optional overrides.
 *
 * <p>This source depends on {@code kafka-clients} / {@code kafka-avro-serializer},
 * which are only present on the classpath when the Engine module is built with
 * the {@code -P kafka} (or {@code -P full}) Maven profile.
 */
public class KafkaOperations extends Command {
    // Producer state (keyed by the engine's per-run key).
    private static final Map<String, String> kafkaServers = new HashMap<>();
    private static final Map<String, String> kafkaProducerTopic = new HashMap<>();
    private static final Map<String, String> kafkaKeySerializer = new HashMap<>();
    private static final Map<String, String> kafkaValueSerializer = new HashMap<>();
    private static final Map<String, Integer> kafkaPartition = new HashMap<>();
    private static final Map<String, String> kafkaKey = new HashMap<>();
    private static final Map<String, Long> kafkaTimeStamp = new HashMap<>();
    private static final Map<String, List<Header>> kafkaHeaders = new HashMap<>();
    private static final Map<String, String> kafkaValue = new HashMap<>();
    private static final Map<String, ProducerRecord<Object, Object>> kafkaProducerRecord = new HashMap<>();
    private static final Map<String, KafkaProducer<Object, Object>> kafkaProducer = new HashMap<>();
    private static final Map<String, Schema> kafkaAvroSchema = new HashMap<>();
    private static final Map<String, String> kafkaSchemaRegistryURL = new HashMap<>();
    private static final Map<String, String> kafkaSharedSecret = new HashMap<>();
    private static final Map<String, Boolean> kafkaAutoRegisterSchemas = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    // Consumer state.
    private static final Map<String, String> kafkaConsumerTopic = new HashMap<>();
    private static final Map<String, String> kafkaConsumerGroupId = new HashMap<>();
    private static final Map<String, String> kafkaValueDeserializer = new HashMap<>();
    private static final Map<String, Integer> kafkaConsumerPollRetries = new HashMap<>();
    private static final Map<String, Long> kafkaConsumerPollDuration = new HashMap<>();
    private static final Map<String, Integer> kafkaConsumerMaxPollRecords = new HashMap<>();
    private static final Map<String, KafkaConsumer<String, Object>> kafkaConsumer = new HashMap<>();
    private static final Map<String, String> kafkaConsumeRecordValue = new HashMap<>();
    private static final Map<String, ConsumerRecord<String, Object>> kafkaConsumerPollRecord = new HashMap<>();
    private static final Map<String, List<HashMap<String, String>>> kafkaRecordIdentifier = new HashMap<>();

    public KafkaOperations(CommandControl cc) {
        super(cc);
    }

    // ------------------------------------------------------------------
    // Named-config resolution (#alias in the Condition column)
    // ------------------------------------------------------------------

    private String aliasFromCondition(String fallback) {
        String cond = Condition == null ? "" : Condition.trim();
        if (cond.startsWith("#")) {
            return cond.substring(1);
        }
        return fallback;
    }

    /** Loads producer config referenced by the Condition alias into per-key state. */
    private Properties resolveProducer() {
        String name = aliasFromCondition("default");
        com.ing.datalib.settings.KafkaProducerProperties store = Control
            .getCurrentProject()
            .getProjectSettings()
            .getKafkaProducerSettings();
        if (!store.doesProducerConfigExist(name)) {
            name = "default";
        }
        Properties cfg = store.getProducerPropertiesFor(name);
        if (cfg == null) {
            cfg = new Properties();
        }
        // Named config seeds any field not already set by a setXxx override.
        kafkaServers.putIfAbsent(key, cfg.getProperty("bootstrap.servers", ""));
        kafkaProducerTopic.putIfAbsent(key, cfg.getProperty("producer.topic", ""));
        kafkaKeySerializer.putIfAbsent(
            key,
            cfg.getProperty("key.serializer", StringSerializer.class.getName())
        );
        kafkaValueSerializer.putIfAbsent(
            key,
            cfg.getProperty("value.serializer", StringSerializer.class.getName())
        );
        String part = cfg.getProperty("partition", "");
        if (!kafkaPartition.containsKey(key) && part != null && !part.trim().isEmpty()) {
            kafkaPartition.put(key, Integer.valueOf(part.trim()));
        }
        // Legacy setXxx overrides take precedence over the named config.
        if (kafkaSchemaRegistryURL.containsKey(key)) {
            cfg.setProperty("schema.registry.url", kafkaSchemaRegistryURL.get(key));
        }
        if (kafkaSharedSecret.containsKey(key)) {
            cfg.setProperty("shared.secret", kafkaSharedSecret.get(key));
        }
        if (kafkaAutoRegisterSchemas.containsKey(key)) {
            cfg.setProperty(
                "auto.register.schemas",
                String.valueOf(kafkaAutoRegisterSchemas.get(key))
            );
        }
        return cfg;
    }

    /** Loads consumer config referenced by the Condition alias into per-key state. */
    private Properties resolveConsumer() {
        String name = aliasFromCondition("default");
        com.ing.datalib.settings.KafkaConsumerProperties store = Control
            .getCurrentProject()
            .getProjectSettings()
            .getKafkaConsumerSettings();
        if (!store.doesConsumerConfigExist(name)) {
            name = "default";
        }
        Properties cfg = store.getConsumerPropertiesFor(name);
        if (cfg == null) {
            cfg = new Properties();
        }
        kafkaServers.putIfAbsent(key, cfg.getProperty("bootstrap.servers", ""));
        kafkaConsumerTopic.putIfAbsent(key, cfg.getProperty("consumer.topic", ""));
        kafkaConsumerGroupId.putIfAbsent(key, cfg.getProperty("group.id", ""));
        kafkaValueDeserializer.putIfAbsent(
            key,
            cfg.getProperty("value.deserializer", StringDeserializer.class.getName())
        );
        kafkaConsumerPollRetries.putIfAbsent(
            key,
            Integer.valueOf(cfg.getProperty("poll.retries", "5"))
        );
        kafkaConsumerPollDuration.putIfAbsent(
            key,
            Long.valueOf(cfg.getProperty("poll.interval.ms", "1000"))
        );
        kafkaConsumerMaxPollRecords.putIfAbsent(
            key,
            Integer.valueOf(cfg.getProperty("max.poll.records", "500"))
        );
        // Legacy setXxx overrides take precedence over the named config.
        if (kafkaSchemaRegistryURL.containsKey(key)) {
            cfg.setProperty("schema.registry.url", kafkaSchemaRegistryURL.get(key));
        }
        if (kafkaSharedSecret.containsKey(key)) {
            cfg.setProperty("shared.secret", kafkaSharedSecret.get(key));
        }
        return cfg;
    }

    /**
     * Maps a short serializer name (string/bytearray/avro) to its class;
     * any other value is treated as a fully-qualified class name and passed through.
     */
    private static String resolveSerializer(String v) {
        if (v == null) {
            return StringSerializer.class.getName();
        }
        switch (v.trim().toLowerCase()) {
            case "string":
                return StringSerializer.class.getName();
            case "bytearray":
                return ByteArraySerializer.class.getName();
            case "avro":
                return "io.confluent.kafka.serializers.KafkaAvroSerializer";
            default:
                return v.trim();
        }
    }

    /** Deserializer counterpart of {@link #resolveSerializer(String)}. */
    private static String resolveDeserializer(String v) {
        if (v == null) {
            return StringDeserializer.class.getName();
        }
        switch (v.trim().toLowerCase()) {
            case "string":
                return StringDeserializer.class.getName();
            case "bytearray":
                return ByteArrayDeserializer.class.getName();
            case "avro":
                return "io.confluent.kafka.serializers.KafkaAvroDeserializer";
            default:
                return v.trim();
        }
    }

    private static boolean isAlias(String v, String alias) {
        return v != null && alias.equalsIgnoreCase(v.trim());
    }

    private static void applySsl(Properties props, Properties cfg) {
        if (Boolean.parseBoolean(cfg.getProperty("ssl.enabled", "false"))) {
            props.put("security.protocol", "SSL");
            // Broker TLS - truststore (server CA) for one-way TLS
            put(
                props,
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
                cfg.getProperty("ssl.truststore.location")
            );
            put(
                props,
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
                cfg.getProperty("ssl.truststore.password")
            );
            put(
                props,
                SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG,
                cfg.getProperty("ssl.truststore.type")
            );
            // Broker TLS - keystore (client cert) for mutual TLS
            put(
                props,
                SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
                cfg.getProperty("ssl.keystore.location")
            );
            put(
                props,
                SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
                cfg.getProperty("ssl.keystore.password")
            );
            put(props, SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, cfg.getProperty("ssl.keystore.type"));
            put(props, SslConfigs.SSL_KEY_PASSWORD_CONFIG, cfg.getProperty("ssl.key.password"));
            // Schema Registry TLS (when schema.registry.url is https)
            put(
                props,
                "schema.registry.ssl.truststore.location",
                cfg.getProperty("schema.registry.ssl.truststore.location")
            );
            put(
                props,
                "schema.registry.ssl.truststore.password",
                cfg.getProperty("schema.registry.ssl.truststore.password")
            );
            put(
                props,
                "schema.registry.ssl.keystore.location",
                cfg.getProperty("schema.registry.ssl.keystore.location")
            );
            put(
                props,
                "schema.registry.ssl.keystore.password",
                cfg.getProperty("schema.registry.ssl.keystore.password")
            );
            put(
                props,
                "schema.registry.ssl.key.password",
                cfg.getProperty("schema.registry.ssl.key.password")
            );
        }
    }

    private static void put(Properties props, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            props.put(key, value);
        }
    }

    // ------------------------------------------------------------------
    // Producer setup overrides (optional; Condition = NONE)
    // ------------------------------------------------------------------

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Bootstrap Servers",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setBootstrapServers() {
        kafkaServers.put(key, Data);
        Report.updateTestLog(Action, "Bootstrap Servers set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Producer Topic",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setProducerTopic() {
        kafkaProducerTopic.put(key, Data);
        Report.updateTestLog(Action, "Producer Topic set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Key Serializer",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setKeySerializer() {
        kafkaKeySerializer.put(key, Data);
        Report.updateTestLog(Action, "Key Serializer set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Value Serializer",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setValueSerializer() {
        kafkaValueSerializer.put(key, Data);
        Report.updateTestLog(Action, "Value Serializer set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Schema Registry URL",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setSchemaRegistryURL() {
        kafkaSchemaRegistryURL.put(key, Data);
        Report.updateTestLog(Action, "Schema Registry URL set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Shared Secret",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setSharedSecret() {
        kafkaSharedSecret.put(key, Data);
        Report.updateTestLog(Action, "Shared Secret set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Auto Register Schemas",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setAutoRegisterSchemas() {
        try {
            kafkaAutoRegisterSchemas.put(key, Boolean.valueOf(Data.trim().toLowerCase()));
            Report.updateTestLog(Action, "Auto Register Schemas set", Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error setting Auto Register Schemas: " + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Partition",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setPartition() {
        try {
            kafkaPartition.put(key, Integer.valueOf(Data));
            Report.updateTestLog(Action, "Partition set", Status.DONE);
        } catch (NumberFormatException ex) {
            Report.updateTestLog(Action, "Invalid partition: " + Data, Status.DEBUG);
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Key",
        input = InputType.YES,
        condition = InputType.NO
    )
    public void setKey() {
        kafkaKey.put(key, Data);
        Report.updateTestLog(Action, "Key set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set TimeStamp",
        input = InputType.NO,
        condition = InputType.NO
    )
    public void setTimeStamp() {
        kafkaTimeStamp.put(key, System.currentTimeMillis());
        Report.updateTestLog(Action, "Time Stamp set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Add Kafka Header",
        input = InputType.YES,
        condition = InputType.NO
    )
    public void addKafkaHeader() {
        try {
            String value = handleDataSheetVariables(Data);
            value = handleUserDefinedVariables(value);
            String headerKey = value.split("=", 2)[0];
            String headerValue = value.split("=", 2)[1];
            kafkaHeaders
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(new RecordHeader(headerKey, headerValue.getBytes()));
            Report.updateTestLog(Action, "Header added " + value, Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error adding Header: " + ex.getMessage(), Status.DEBUG);
        }
    }

    // ------------------------------------------------------------------
    // Produce / Send (Condition = optional #producerAlias)
    // ------------------------------------------------------------------

    @Action(
        object = ObjectType.KAFKA,
        desc = "Add Avro Schema",
        input = InputType.YES,
        condition = InputType.NO
    )
    public void addSchema() {
        try {
            String data = handleDataSheetVariables(Data);
            data = handleUserDefinedVariables(data);
            Schema.Parser parser = new Schema.Parser();
            Schema mainSchema;
            if (data.contains(";")) {
                // Multiple ';'-separated files: dependent schemas first, main schema last.
                String[] paths = data.split(";");
                for (int i = 0; i < paths.length - 1; i++) {
                    parser.parse(new File(Paths.get(paths[i].trim()).toString()));
                }
                mainSchema =
                    parser.parse(new File(Paths.get(paths[paths.length - 1].trim()).toString()));
            } else {
                mainSchema = new Schema.Parser().parse(new File(Paths.get(data.trim()).toString()));
            }
            kafkaAvroSchema.put(key, mainSchema);
            Report.updateTestLog(Action, "Schema added successfully", Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Unable to add Schema: " + ex.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Produce Kafka Message",
        input = InputType.YES,
        condition = InputType.OPTIONAL
    )
    public void produceMessage() {
        try {
            resolveProducer();
            String value = handleDataSheetVariables(Data);
            value = handleUserDefinedVariables(value);
            kafkaValue.put(key, value);

            // Local-schema Avro: convert the JSON payload into a GenericRecord when
            // an .avsc was supplied via "Add Avro Schema" and the avro serializer
            // is selected. Otherwise the raw String value is sent.
            Object recordValue = value;
            String serializer = kafkaValueSerializer.get(key);
            if (kafkaAvroSchema.containsKey(key) && isAlias(serializer, "avro")) {
                recordValue = buildGenericRecord(value);
            }

            String topic = kafkaProducerTopic.get(key);
            Integer partition = kafkaPartition.get(key);
            String msgKey = kafkaKey.get(key);
            Long ts = kafkaTimeStamp.get(key);
            List<Header> headers = kafkaHeaders.get(key);

            ProducerRecord<Object, Object> record;
            if (partition != null && ts != null) {
                record = new ProducerRecord<>(topic, partition, ts, msgKey, recordValue, headers);
            } else if (partition != null) {
                record = new ProducerRecord<>(topic, partition, msgKey, recordValue, headers);
            } else if (msgKey != null) {
                record = new ProducerRecord<>(topic, null, msgKey, recordValue, headers);
            } else {
                record = new ProducerRecord<>(topic, recordValue);
            }
            kafkaProducerRecord.put(key, record);
            Report.updateTestLog(Action, "Message prepared for topic [" + topic + "]", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error producing message: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Send Message",
        input = InputType.NO,
        condition = InputType.OPTIONAL
    )
    public void sendKafkaMessage() {
        try {
            Properties cfg = resolveProducer();
            createProducer(cfg);
            kafkaProducer
                .get(key)
                .send(
                    kafkaProducerRecord.get(key),
                    (RecordMetadata metadata, Exception exception) -> {
                        if (exception != null) {
                            Report.updateTestLog(
                                Action,
                                "Error sending record: " + exception.getMessage(),
                                Status.FAIL
                            );
                        } else {
                            Report.updateTestLog(
                                Action,
                                "Record sent [topic: " +
                                metadata.topic() +
                                ", partition: " +
                                metadata.partition() +
                                ", offset: " +
                                metadata.offset() +
                                "]",
                                Status.DONE
                            );
                        }
                    }
                );
            kafkaProducer.get(key).flush();
            kafkaProducer.get(key).close();
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "send failed", ex);
            Report.updateTestLog(Action, "Error sending record: " + ex.getMessage(), Status.DEBUG);
        } finally {
            clearProducerDetails();
        }
    }

    private void createProducer(Properties cfg) {
        Properties props = new Properties();
        applySsl(props, cfg);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers.get(key));
        props.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            resolveSerializer(kafkaKeySerializer.get(key))
        );
        props.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            resolveSerializer(kafkaValueSerializer.get(key))
        );
        String schemaUrl = cfg.getProperty("schema.registry.url", "");
        if (!schemaUrl.trim().isEmpty()) {
            props.put("schema.registry.url", schemaUrl);
            put(props, "auto.register.schemas", cfg.getProperty("auto.register.schemas"));
        }
        kafkaProducer.put(key, new KafkaProducer<>(props));
    }

    private void clearProducerDetails() {
        kafkaKey.remove(key);
        kafkaHeaders.remove(key);
        kafkaProducerTopic.remove(key);
        kafkaPartition.remove(key);
        kafkaTimeStamp.remove(key);
        kafkaKeySerializer.remove(key);
        kafkaValue.remove(key);
        kafkaValueSerializer.remove(key);
        kafkaProducer.remove(key);
        kafkaProducerRecord.remove(key);
        kafkaAvroSchema.remove(key);
        kafkaServers.remove(key);
        kafkaSchemaRegistryURL.remove(key);
        kafkaSharedSecret.remove(key);
        kafkaAutoRegisterSchemas.remove(key);
    }

    // ------------------------------------------------------------------
    // Local-schema Avro helpers (JSON payload -> GenericRecord)
    // ------------------------------------------------------------------

    private GenericRecord buildGenericRecord(String jsonValue) throws Exception {
        Schema schema = kafkaAvroSchema.get(key);
        JsonNode inputJson = mapper.readTree(jsonValue);
        JsonNode avroCompatibleJson = convertNode(inputJson, schema);
        String jsonAvroMessage = mapper.writeValueAsString(avroCompatibleJson);
        InputStream input = new ByteArrayInputStream(jsonAvroMessage.getBytes());
        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, input);
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        return reader.read(null, decoder);
    }

    /** Normalises a JSON tree into Avro-union-compatible form for the given schema. */
    private static JsonNode convertNode(JsonNode input, Schema schema) {
        switch (schema.getType()) {
            case RECORD:
                ObjectNode recordNode = mapper.createObjectNode();
                for (Schema.Field field : schema.getFields()) {
                    JsonNode value = input.get(field.name());
                    recordNode.set(field.name(), convertNode(value, field.schema()));
                }
                return recordNode;
            case ARRAY:
                ArrayNode arrayNode = mapper.createArrayNode();
                for (JsonNode item : input) {
                    arrayNode.add(convertNode(item, schema.getElementType()));
                }
                return arrayNode;
            case MAP:
                ObjectNode mapNode = mapper.createObjectNode();
                for (Iterator<Map.Entry<String, JsonNode>> it = input.fields(); it.hasNext();) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    mapNode.set(
                        entry.getKey(),
                        convertNode(entry.getValue(), schema.getValueType())
                    );
                }
                return mapNode;
            case UNION:
                for (Schema subSchema : schema.getTypes()) {
                    if (
                        subSchema.getType() == Schema.Type.NULL && (input == null || input.isNull())
                    ) {
                        return NullNode.getInstance();
                    }
                    if (isCompatible(input, subSchema)) {
                        JsonNode wrapped = convertNode(input, subSchema);
                        ObjectNode unionNode = mapper.createObjectNode();
                        String typeName = (
                                subSchema.getType() == Schema.Type.RECORD ||
                                subSchema.getType() == Schema.Type.ENUM
                            )
                            ? subSchema.getFullName()
                            : subSchema.getType().getName();
                        unionNode.set(typeName, wrapped);
                        return unionNode;
                    }
                }
                throw new IllegalArgumentException("No matching type in union for value: " + input);
            case ENUM:
                return new TextNode(input.textValue());
            default:
                return input;
        }
    }

    private static boolean isCompatible(JsonNode value, Schema schema) {
        switch (schema.getType()) {
            case STRING:
                return value.isTextual();
            case INT:
                return value.isInt();
            case LONG:
                return value.isLong() || value.isInt();
            case FLOAT:
                return value.isFloat() || value.isDouble();
            case DOUBLE:
                return value.isDouble() || value.isFloat();
            case BOOLEAN:
                return value.isBoolean();
            case NULL:
                return value == null || value.isNull();
            case RECORD:
                return value.isObject();
            case ARRAY:
                return value.isArray();
            case MAP:
                return value.isObject();
            case ENUM:
                return value.isTextual() && schema.getEnumSymbols().contains(value.textValue());
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------
    // Consumer setup overrides (optional; Condition = NONE)
    // ------------------------------------------------------------------

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Consumer Topic",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setConsumerTopic() {
        kafkaConsumerTopic.put(key, Data);
        Report.updateTestLog(Action, "Consumer Topic set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Consumer GroupId",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setConsumerGroupId() {
        kafkaConsumerGroupId.put(key, Data);
        Report.updateTestLog(Action, "Consumer GroupId set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Value Deserializer",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setValueDeserializer() {
        kafkaValueDeserializer.put(key, Data);
        Report.updateTestLog(Action, "Value Deserializer set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Consumer Poll Retries",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setConsumerPollRetries() {
        kafkaConsumerPollRetries.put(key, Integer.valueOf(Data));
        Report.updateTestLog(Action, "Poll Retries set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Consumer Poll Interval",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setConsumerPollInterval() {
        kafkaConsumerPollDuration.put(key, Long.valueOf(Data));
        Report.updateTestLog(Action, "Poll Interval set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Set Consumer Max Poll Records",
        input = InputType.YES,
        condition = InputType.NO,
        deprecated = true
    )
    public void setConsumerMaxPollRecords() {
        kafkaConsumerMaxPollRecords.put(key, Integer.valueOf(Data));
        Report.updateTestLog(Action, "Max Poll Records set", Status.DONE);
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Identify Target Message",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void identifyTargetMessage() {
        try {
            HashMap<String, String> pathValue = new HashMap<>();
            pathValue.put(Condition, Data);
            kafkaRecordIdentifier.computeIfAbsent(key, k -> new ArrayList<>()).add(pathValue);
            Report.updateTestLog(
                Action,
                "Target identifier added [" + Condition + "=" + Data + "]",
                Status.DONE
            );
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error in target setup: " + ex.getMessage(), Status.FAIL);
        }
    }

    // ------------------------------------------------------------------
    // Consume (Condition = optional #consumerAlias)
    // ------------------------------------------------------------------

    @Action(
        object = ObjectType.KAFKA,
        desc = "Consume Kafka Message",
        input = InputType.NO,
        condition = InputType.OPTIONAL
    )
    public void consumeKafkaMessage() {
        try {
            Properties cfg = resolveConsumer();
            createConsumer(cfg);
            kafkaConsumer.get(key).subscribe(Arrays.asList(kafkaConsumerTopic.get(key)));
            ConsumerRecords<String, Object> records = pollKafkaConsumer();
            boolean hasIdentifier = kafkaRecordIdentifier.containsKey(key);
            if (records != null && kafkaConsumeRecordValue.containsKey(key)) {
                Report.updateTestLog(
                    Action,
                    "Kafka message consumed and target found.",
                    Status.DONE
                );
            } else if (records != null && hasIdentifier) {
                Report.updateTestLog(
                    Action,
                    "Consumed messages but target not found.",
                    Status.FAILNS
                );
            } else if (records != null) {
                Report.updateTestLog(Action, "Kafka message consumed.", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Kafka message not received.", Status.FAIL);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error consuming Kafka message: " + ex.getMessage(),
                Status.FAIL
            );
        } finally {
            if (kafkaConsumer.get(key) != null) {
                kafkaConsumer.get(key).close();
            }
        }
    }

    private void createConsumer(Properties cfg) {
        Properties props = new Properties();
        applySsl(props, cfg);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers.get(key));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerGroupId.get(key));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            resolveDeserializer(kafkaValueDeserializer.get(key))
        );
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        if (kafkaConsumerMaxPollRecords.get(key) != null) {
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaConsumerMaxPollRecords.get(key));
        }
        String schemaUrl = cfg.getProperty("schema.registry.url", "");
        if (!schemaUrl.trim().isEmpty()) {
            props.put("schema.registry.url", schemaUrl);
        }
        String valDes = kafkaValueDeserializer.get(key);
        if (isAlias(valDes, "avro")) {
            props.put("specific.avro.reader", "false");
        }
        kafkaConsumer.put(key, new KafkaConsumer<>(props));
    }

    private ConsumerRecords<String, Object> pollKafkaConsumer() {
        int maxRetries = kafkaConsumerPollRetries.get(key);
        long pollMs = kafkaConsumerPollDuration.get(key);
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            ConsumerRecords<String, Object> polled = kafkaConsumer
                .get(key)
                .poll(Duration.ofMillis(pollMs));
            if (!polled.isEmpty()) {
                for (ConsumerRecord<String, Object> record : polled) {
                    kafkaConsumerPollRecord.put(key, record);
                    if (findAndSetTargetRecord()) {
                        return polled;
                    }
                }
                if (!kafkaRecordIdentifier.containsKey(key)) {
                    // No identifier configured: first non-empty batch is the result.
                    ConsumerRecord<String, Object> first = polled.iterator().next();
                    if (first.value() != null) {
                        kafkaConsumeRecordValue.put(key, first.value().toString());
                    }
                    return polled;
                }
            }
        }
        return null;
    }

    private boolean findAndSetTargetRecord() {
        try {
            Object val = kafkaConsumerPollRecord.get(key).value();
            if (val == null) {
                return false;
            }
            String recordValue = val.toString();
            boolean isJson = Pattern.matches("^\\s*(\\{.*\\}|\\[.*\\])\\s*$", recordValue);
            boolean isXml = Pattern.matches("^\\s*<\\?*xml.*>.*<.*>.*</.*>\\s*$", recordValue);
            if (isJson) {
                return matchJson(recordValue);
            } else if (isXml) {
                return matchXml(recordValue);
            }
        } catch (Exception ex) {
            System.out.println("Error matching record: " + ex.getMessage());
        }
        return false;
    }

    private boolean matchJson(String message) {
        List<HashMap<String, String>> conditions = kafkaRecordIdentifier.get(key);
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        for (HashMap<String, String> cond : conditions) {
            Map.Entry<String, String> entry = cond.entrySet().iterator().next();
            Object actualObj = JsonPath.read(message, entry.getKey());
            String actual = actualObj == null ? null : String.valueOf(actualObj);
            if (!Objects.equals(actual, entry.getValue())) {
                return false;
            }
        }
        kafkaConsumeRecordValue.put(key, message);
        return true;
    }

    private boolean matchXml(String message) {
        try {
            List<HashMap<String, String>> conditions = kafkaRecordIdentifier.get(key);
            if (conditions == null || conditions.isEmpty()) {
                return false;
            }
            Document doc = parseXml(message);
            XPath xPath = XPathFactory.newInstance().newXPath();
            for (HashMap<String, String> cond : conditions) {
                Map.Entry<String, String> entry = cond.entrySet().iterator().next();
                String actual = xPath.compile(entry.getKey()).evaluate(doc);
                if (!Objects.equals(actual, entry.getValue())) {
                    return false;
                }
            }
            kafkaConsumeRecordValue.put(key, message);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Close Consumer",
        input = InputType.NO,
        condition = InputType.NO
    )
    public void closeConsumer() {
        kafkaConsumeRecordValue.remove(key);
        kafkaConsumerPollDuration.remove(key);
        kafkaConsumerPollRetries.remove(key);
        kafkaConsumerTopic.remove(key);
        kafkaValueDeserializer.remove(key);
        kafkaConsumerGroupId.remove(key);
        kafkaConsumerMaxPollRecords.remove(key);
        kafkaConsumerPollRecord.remove(key);
        kafkaRecordIdentifier.remove(key);
        kafkaConsumer.remove(key);
        kafkaServers.remove(key);
        kafkaSchemaRegistryURL.remove(key);
        kafkaSharedSecret.remove(key);
        Report.updateTestLog(Action, "Consumer closed", Status.DONE);
    }

    // ------------------------------------------------------------------
    // Assertions on the consumed message
    // ------------------------------------------------------------------

    @Action(
        object = ObjectType.KAFKA,
        desc = "Assert Response Message Contains",
        input = InputType.YES,
        condition = InputType.NO
    )
    public void assertKafkaResponseMessageContains() {
        String response = kafkaConsumeRecordValue.get(key);
        if (response != null && response.contains(Data)) {
            Report.updateTestLog(Action, "Response contains: " + Data, Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "Response does not contain: " + Data, Status.FAILNS);
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Assert JSON Tag Equals",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void assertKafkaJSONtagEquals() {
        try {
            String value = JsonPath.read(kafkaConsumeRecordValue.get(key), Condition).toString();
            if (value.equals(Data)) {
                Report.updateTestLog(
                    Action,
                    "Element [" + value + "] is as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element is [" + value + "] expected [" + Data + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error validating JSON element: " + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Assert JSON Tag Contains",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void assertKafkaJSONtagContains() {
        try {
            String value = JsonPath.read(kafkaConsumeRecordValue.get(key), Condition).toString();
            if (value.contains(Data)) {
                Report.updateTestLog(Action, "Element contains [" + Data + "]", Status.PASSNS);
            } else {
                Report.updateTestLog(
                    Action,
                    "Element [" + value + "] does not contain [" + Data + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error validating JSON element: " + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Assert XML Tag Equals",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void assertKafkaXMLtagEquals() {
        try {
            String value = XPathFactory
                .newInstance()
                .newXPath()
                .compile(Condition)
                .evaluate(parseXml(kafkaConsumeRecordValue.get(key)));
            if (value.equals(Data)) {
                Report.updateTestLog(
                    Action,
                    "Element [" + value + "] is as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element [" + value + "] not as expected",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error validating XML element: " + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Assert XML Tag Contains",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void assertKafkaXMLtagContains() {
        try {
            String value = XPathFactory
                .newInstance()
                .newXPath()
                .compile(Condition)
                .evaluate(parseXml(kafkaConsumeRecordValue.get(key)));
            if (value.contains(Data)) {
                Report.updateTestLog(Action, "Element contains [" + Data + "]", Status.PASSNS);
            } else {
                Report.updateTestLog(
                    Action,
                    "Element [" + value + "] does not contain [" + Data + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error validating XML element: " + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Store Response In DataSheet",
        input = InputType.YES,
        condition = InputType.NO
    )
    public void storeKafkaResponseInDataSheet() {
        storeInDataSheet(kafkaConsumeRecordValue.get(key));
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Store JSON Tag In DataSheet",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void storeKafkaJSONtagInDataSheet() {
        try {
            storeInDataSheet(JsonPath.read(kafkaConsumeRecordValue.get(key), Condition).toString());
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error storing JSON element: " + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    @Action(
        object = ObjectType.KAFKA,
        desc = "Store XML Tag In DataSheet",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void storeKafkaXMLtagInDataSheet() {
        try {
            String value = XPathFactory
                .newInstance()
                .newXPath()
                .compile(Condition)
                .evaluate(parseXml(kafkaConsumeRecordValue.get(key)));
            storeInDataSheet(value);
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error storing XML element: " + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    private void storeInDataSheet(String value) {
        String strObj = Input;
        if (strObj != null && strObj.matches(".*:.*")) {
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, value);
            Report.updateTestLog(Action, "[" + value + "] stored in " + strObj, Status.DONE);
        } else {
            Report.updateTestLog(
                Action,
                "Invalid input [" + Input + "]; expected [sheetName:ColumnName]",
                Status.DEBUG
            );
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private String handleDataSheetVariables(String payload) {
        if (payload == null) {
            return null;
        }
        List<String> sheets = Control
            .getCurrentProject()
            .getTestData()
            .getTestDataFor(Control.exe.runEnv())
            .getTestDataNames();
        for (String sheet : sheets) {
            if (payload.contains("{" + sheet + ":")) {
                com.ing.datalib.testdata.model.TestDataModel td = Control
                    .getCurrentProject()
                    .getTestData()
                    .getTestDataByName(sheet);
                for (String col : td.getColumns()) {
                    String token = "{" + sheet + ":" + col + "}";
                    if (payload.contains(token)) {
                        payload = payload.replace(token, userData.getData(sheet, col));
                    }
                }
            }
        }
        return payload;
    }

    private String handleUserDefinedVariables(String payload) {
        if (payload == null) {
            return null;
        }
        Collection<Object> values = Control
            .getCurrentProject()
            .getProjectSettings()
            .getUserDefinedSettings()
            .values();
        for (Object prop : values) {
            if (payload.contains("{" + prop + "}")) {
                payload = payload.replace("{" + prop + "}", prop.toString());
            }
        }
        return payload;
    }
}

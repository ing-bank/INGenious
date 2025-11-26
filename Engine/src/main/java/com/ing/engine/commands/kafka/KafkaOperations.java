/**  Kafka Operations related commands

package com.ing.engine.commands.kafka;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.jayway.jsonpath.JsonPath;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.io.ByteArrayInputStream;
import java.io.File;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.clients.consumer.*;
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
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class KafkaOperations extends General {

private final static ObjectMapper mapper = new ObjectMapper();

    public KafkaOperations(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.KAFKA, desc = "Add Kafka Header", input = InputType.YES)
    public void addKafkaHeader() {
        try {

            List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                    .getTestDataNames();
            for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
                if (Data.contains("{" + sheetlist.get(sheet) + ":")) {
                    com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject().getTestData()
                            .getTestDataByName(sheetlist.get(sheet));
                    List<String> columns = tdModel.getColumns();
                    for (int col = 0; col < columns.size(); col++) {
                        if (Data.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                            Data = Data.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                    userData.getData(sheetlist.get(sheet), columns.get(col)));
                        }
                    }
                }
            }

            Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                    .values();
            for (Object prop : valuelist) {
                if (Data.contains("{" + prop + "}")) {
                    Data = Data.replace("{" + prop + "}", prop.toString());
                }
            }
            String headerKey = Data.split("=", 2)[0];
            String headerValue = Data.split("=", 2)[1];

            if (kafkaHeaders.containsKey(key)) {
                kafkaHeaders.get(key).add(new RecordHeader(headerKey, headerValue.getBytes()));
            } else {
                ArrayList<Header> toBeAdded = new ArrayList<Header>();
                toBeAdded.add(new RecordHeader(headerKey, headerValue.getBytes()));
                kafkaHeaders.put(key, toBeAdded);
            }

            Report.updateTestLog(Action, "Header added " + Data, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error adding Header :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Producer Topic", input = InputType.YES, condition = InputType.NO)
    public void setProducerTopic() {
        try {
            kafkaProducerTopic.put(key, Data);
            Report.updateTestLog(Action, "Topic has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Topic setup", ex);
            Report.updateTestLog(Action, "Error in setting Topic: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Auto Register Schemas", input = InputType.YES, condition = InputType.NO)
    public void setAutoRegisterSchemas() {
        try {
            kafkaAutoRegisterSchemas.put(key, Boolean.valueOf(Data.toLowerCase().trim()));
            Report.updateTestLog(Action, "Auto Register Schemas has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception Max Poll Record setup", ex);
            Report.updateTestLog(Action, "Error in Auto Register Schemas: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Consumer Topic", input = InputType.YES, condition = InputType.NO)
    public void setConsumerTopic() {
        try {
            kafkaConsumerTopic.put(key, Data);
            Report.updateTestLog(Action, "Topic has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Topic setup", ex);
            Report.updateTestLog(Action, "Error in setting Topic: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Consumer Retries", input = InputType.YES, condition = InputType.NO)
    public void setConsumerPollRetries() {
        try {
            kafkaConsumerPollRetries.put(key, Integer.parseInt(Data));
            Report.updateTestLog(Action, "Poll Retries has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Poll Retries setup", ex);
            Report.updateTestLog(Action, "Error in setting Poll Retries: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Consumer Retries", input = InputType.YES, condition = InputType.NO)
    public void setConsumerPollInterval() {
        try {
            kafkaConsumerPollDuration.put(key, Long.valueOf(Data));
            Report.updateTestLog(Action, "Poll interval has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Poll interval setup", ex);
            Report.updateTestLog(Action, "Error in setting Poll interval: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Consumer Max Poll Records", input = InputType.YES, condition = InputType.NO)
    public void setConsumerMaxPollRecords() {
        try {
            kafkaConsumerMaxPollRecords.put(key, Integer.valueOf(Data));
            Report.updateTestLog(Action, "Max Poll Records has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception Max Poll Record setup", ex);
            Report.updateTestLog(Action, "Error in setting Max Poll Records: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Bootstrap Servers", input = InputType.YES, condition = InputType.NO)
    public void setBootstrapServers() {
        try {
            kafkaServers.put(key, Data);
            Report.updateTestLog(Action, "Bootstrap Servers have been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Bootstrap Servers setup",
                    ex);
            Report.updateTestLog(Action, "Error in setting Bootstrap Servers: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Schema Registry URL", input = InputType.YES, condition = InputType.NO)
    public void setSchemaRegistryURL() {
        try {
            kafkaSchemaRegistryURL.put(key, Data);
            Report.updateTestLog(Action, "Schema Registry URL has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Schema Registry URL setup",
                    ex);
            Report.updateTestLog(Action, "Error in setting Schema Registry URL: " + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Shared Secret", input = InputType.YES, condition = InputType.NO)
    public void setSharedSecret() {
        try {
            kafkaSharedSecret.put(key, Data);
            Report.updateTestLog(Action, "Shared Secret set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Shared Secret setup", ex);
            Report.updateTestLog(Action, "Error in setting Shared Secret: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Key", input = InputType.YES, condition = InputType.NO)
    public void setKey() {
        try {
            kafkaKey.put(key, Data);
            Report.updateTestLog(Action, "Key has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Key setup", ex);
            Report.updateTestLog(Action, "Error in setting Key: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Consumer GroupId", input = InputType.YES, condition = InputType.NO)
    public void setConsumerGroupId() {
        try {
            kafkaConsumerGroupId.put(key, Data);
            Report.updateTestLog(Action, "Consumer GroupId has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Consumer GroupId setup",
                    ex);
            Report.updateTestLog(Action, "Error in setting Consumer GroupId: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Partition", input = InputType.YES, condition = InputType.NO)
    public void setPartition() {
        try {
            if (Data.toLowerCase().equals("null")) {
                kafkaPartition.put(key, null);
            } else {
                kafkaPartition.put(key, Integer.valueOf(Data));
            }
            Report.updateTestLog(Action, "Partition has been set successfully", Status.DONE);
        } catch (NumberFormatException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Partition setup", ex);
            Report.updateTestLog(Action, "Error in setting Partition: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set TimeStamp", input = InputType.NO, condition = InputType.NO)
    public void setTimeStamp() {
        try {
            kafkaTimeStamp.put(key, System.currentTimeMillis());
            Report.updateTestLog(Action, "Time Stamp has been set successfully", Status.DONE);
        } catch (NumberFormatException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Time Stamp setup", ex);
            Report.updateTestLog(Action, "Error in setting Time Stamp: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Key Serializer", input = InputType.YES, condition = InputType.NO)
    public void setKeySerializer() {
        try {
            kafkaKeySerializer.put(key, Data);
            Report.updateTestLog(Action, "Key Serializer has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Key Serializer setup", ex);
            Report.updateTestLog(Action, "Error in setting Key Serializer: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Value Serializer", input = InputType.YES, condition = InputType.NO)
    public void setValueSerializer() {
        try {
            kafkaValueSerializer.put(key, Data);
            Report.updateTestLog(Action, "Value Serializer has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Value Serializer setup",
                    ex);
            Report.updateTestLog(Action, "Error in setting Value Serializer: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Value Deserializer", input = InputType.YES, condition = InputType.NO)
    public void setValueDeserializer() {
        try {
            kafkaValueDeserializer.put(key, Data);
            Report.updateTestLog(Action, "Value Deserializer has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Value Deserializer setup",
                    ex);
            Report.updateTestLog(Action, "Error in setting Value Deserializer: " + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Add Avro Schema", input = InputType.YES, condition = InputType.NO)
    public void addSchema() throws IOException {
        try {
            Schema mainSchema = null;
            Schema.Parser parser = new Schema.Parser();
            if (Data.contains(";")) {
                String[] paths = Data.split(";");
                for (int i = 0; i < paths.length - 1; i++) {

                    parser.parse(new File(Paths.get(paths[i]).toString()));
                }
                mainSchema = parser.parse(new File(Paths.get(paths[paths.length - 1]).toString()));

            } else {
                // Only one schema, no dependencies
                mainSchema = new Schema.Parser().parse(new File(Paths.get(Data).toString()));
            }
            kafkaAvroSchema.put(key, mainSchema);
            Report.updateTestLog(Action, "Schema added successfully", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, " Unable to add Schema : " + e.getMessage(), Status.FAIL);
        }

    }

    @Action(object = ObjectType.KAFKA, desc = "Produce Kafka Message", input = InputType.YES, condition = InputType.NO)
    public void produceMessage() {
        try {
            String value = Data;
            value = handleDataSheetVariables(value);
            value = handleuserDefinedVariables(value);
            System.out.println("\n Generated Record is : \n " + value + "\n");
            kafkaValue.put(key, value);
            if (kafkaValueSerializer.get(key).equals("avro")) {
                getAvroCompatibleMessage();
                kafkaValue.put(key, kafkaAvroCompatibleMessage.get(key));
                produceGenericRecord(kafkaValue.get(key));
            }
            if (kafkaHeaders.get(key) != null && kafkaTimeStamp.get(key) != null) {
                produceMessage(kafkaProducerTopic.get(key), kafkaPartition.get(key), kafkaTimeStamp.get(key),
                        kafkaKey.get(key), kafkaValue.get(key), kafkaHeaders.get(key));
            } else if (kafkaHeaders.get(key) != null) {
                produceMessage(kafkaProducerTopic.get(key), kafkaPartition.get(key), kafkaKey.get(key),
                        kafkaValue.get(key), kafkaHeaders.get(key));
            } else if (kafkaTimeStamp.get(key) != null) {
                produceMessage(kafkaProducerTopic.get(key), kafkaPartition.get(key), kafkaTimeStamp.get(key),
                        kafkaKey.get(key), kafkaValue.get(key));
            } else if (kafkaPartition.containsKey(key)) {
                produceMessage(kafkaProducerTopic.get(key), kafkaPartition.get(key), kafkaKey.get(key),
                        kafkaValue.get(key));
            } else if (kafkaKey.get(key) != null) {
                produceMessage(kafkaProducerTopic.get(key), kafkaKey.get(key), kafkaValue.get(key));
            } else {
                produceMessage(kafkaProducerTopic.get(key), kafkaValue.get(key));
            }

            Report.updateTestLog(Action, "Message has been produced. ", Status.DONE);

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Something went wrong in producing the message" + "\n" + ex.getMessage(),
                    Status.FAILNS);
            ex.printStackTrace();
        }
    }

    public void produceGenericRecord(Object message) {
        try {
            InputStream input = new ByteArrayInputStream(((String) message).getBytes());
            Decoder decoder = DecoderFactory.get().jsonDecoder(kafkaAvroSchema.get(key), input);
            GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(kafkaAvroSchema.get(key));
            GenericRecord record = reader.read(null, decoder);
            kafkaValue.put(key, record);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Action(object = ObjectType.KAFKA, desc = "Send Message", input = InputType.NO, condition = InputType.NO)
    public void sendKafkaMessage() {
        try {
            createProducer(kafkaValueSerializer.get(key));

            kafkaProducer.get(key).send(kafkaProducerRecord.get(key),
                    (RecordMetadata metadata, Exception exception) -> {
                        if (exception != null) {
                            Report.updateTestLog(Action, "Error in sending record : " + exception.getMessage(),
                                    Status.FAIL);
                        } else {
                            Report.updateTestLog(Action,
                                    "Record sent to [topic: " + metadata.topic() + ", partition: "
                                    + metadata.partition() + ", offset: " + metadata.offset() + ", timestamp: "
                                    + metadata.timestamp() + "]",
                                    Status.DONE);
                        }
                    });

            kafkaProducer.get(key).close();
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception while sending record", ex);
            Report.updateTestLog(Action, "Error in sending record: " + "\n" + ex.getMessage(), Status.DEBUG);
        } finally {
            clearProducerDetails();
        }
    }

    private void createProducer(String serializer) {
//        getProducersslConfigurations();
        Properties props = new Properties();
        if (isProducersslEnabled()) {
            props = getProducersslConfigurations(props);
            props.put("security.protocol", "SSL");
        }
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers.get(key));
        if (kafkaConfigs.containsKey(key)) {
            props = addConfigProps(props);
        }
        if (serializer.toLowerCase().contains("string")) {
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        } else if (serializer.toLowerCase().contains("bytearray")) {
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        } else if (serializer.toLowerCase().contains("avro")) {
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
            props.put("schema.registry.url", kafkaSchemaRegistryURL.get(key));
            if (kafkaAutoRegisterSchemas.get(key) != null) {
                props.put("auto.register.schemas", kafkaAutoRegisterSchemas.get(key));
            }

        } else {
            throw new IllegalArgumentException("Unsupported value type");
        }

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer.put(key, new KafkaProducer<>(props));
    }

    private void produceMessage(String topic, Object value) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, value));
    }

    private void produceMessage(String topic, String kafkaKey, Object value) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, kafkaKey, value));
    }

    private void produceMessage(String topic, Integer partition, String kafkaKey, Object value) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, partition, kafkaKey, value));
    }

    private void produceMessage(String topic, Integer partition, long timestamp, String kafkaKey, Object value) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, partition, timestamp, kafkaKey, value));
    }

    private void produceMessage(String topic, Integer partition, String kafkaKey, Object value, List<Header> headers) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, partition, kafkaKey, value, headers));
    }

    private void produceMessage(String topic, Integer partition, long timestamp, String kafkaKey, Object value,
            List<Header> headers) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, partition, timestamp, kafkaKey, value, headers));
    }

    private String handleDataSheetVariables(String payloadstring) {
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (payloadstring.contains("{" + sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject().getTestData()
                        .getTestDataByName(sheetlist.get(sheet));
                List<String> columns = tdModel.getColumns();
                for (int col = 0; col < columns.size(); col++) {
                    if (payloadstring.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                        payloadstring = payloadstring.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                userData.getData(sheetlist.get(sheet), columns.get(col)));
                    }
                }
            }
        }
        return payloadstring;
    }

    private String handleuserDefinedVariables(String payloadstring) {
        Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                .values();
        for (Object prop : valuelist) {
            if (payloadstring.contains("{" + prop + "}")) {
                payloadstring = payloadstring.replace("{" + prop + "}", prop.toString());
            }
        }
        return payloadstring;
    }

    private void clearProducerDetails() {
        kafkaKey.clear();
        kafkaHeaders.clear();
        kafkaProducerTopic.clear();
        kafkaPartition.clear();
        kafkaTimeStamp.clear();
        kafkaKeySerializer.clear();
        kafkaValue.clear();
        kafkaValueSerializer.clear();
        kafkaProducer.clear();
        kafkaProducerRecord.clear();
        kafkaAvroSchema.clear();
        kafkaGenericRecord.clear();
        kafkaAvroProducer.clear();
        kafkaConfigs.clear();
        kafkaProducersslConfigs.clear();
        kafkaAvroCompatibleMessage.clear();
        kafkaSharedSecret.clear();
        kafkaAutoRegisterSchemas.clear();
    }

    public void createConsumer(String deserializer) {
        try {
            Properties props = new Properties();
            if (isConsumersslEnabled()) {
                props = getConsumersslConfigurations(props);
                props.put("security.protocol", "SSL");
            }
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers.get(key));
            props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerGroupId.get(key));
            if (kafkaConsumerMaxPollRecords.get(key) != null) {
                props.put("max.poll.records", kafkaConsumerMaxPollRecords.get(key));
            }
            if (deserializer.toLowerCase().contains("string")) {
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            } else if (deserializer.toLowerCase().contains("bytearray")) {
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
            } else if (deserializer.toLowerCase().contains("avro")) {
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
                props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "false");
                props.put("schema.registry.url", kafkaSchemaRegistryURL.get(key));

            } else {
                throw new IllegalArgumentException("Unsupported value type");
            }

            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            kafkaConsumer.put(key, new KafkaConsumer<>(props));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Consume Kafka Message", input = InputType.NO)
    public void consumeKafkaMessage() {
        try {
            createConsumer(kafkaValueDeserializer.get(key));
            kafkaConsumer.get(key).subscribe(Arrays.asList(kafkaConsumerTopic.get(key)));
            ConsumerRecords record = pollKafkaConsumer();
            if (record != null && kafkaConsumeRecordValue.containsKey(key)) {
                Report.updateTestLog(Action, "Kafka messages consumed successfully and Target message found.",
                        Status.DONE);
            } else if (record != null && !kafkaConsumeRecordValue.containsKey(key)
                    && kafkaConsumerPollRecord.containsKey(key)) {
                Report.updateTestLog(Action, "Kafka messages consumed successfully but target message not found.",
                        Status.FAILNS);
            } else {
                Report.updateTestLog(Action, "Kafka message not received.", Status.FAIL);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Report.updateTestLog(Action, "Error while consuming Kafka message: " + e.getMessage(), Status.FAIL);
        } finally {
            kafkaConsumer.get(key).close();
        }
    }

    private ConsumerRecords<String, Object> pollKafkaConsumer() throws SerializationException {
        int maxRetries = kafkaConsumerPollRetries.get(key);
        int attempt = 1;
        boolean matchRecordFound = false;
        List<ConsumerRecord<String, Object>> allRecords = new ArrayList<>();

        while (attempt <= maxRetries) {
            try {
                ConsumerRecords<String, Object> pollRecords = kafkaConsumer.get(key)
                        .poll(Duration.ofMillis(kafkaConsumerPollDuration.get(key)));
                if (!pollRecords.isEmpty()) {
                    for (ConsumerRecord<String, Object> record : pollRecords) {
                        kafkaConsumerPollRecord.put(key, record);
                        allRecords.add(record);
                        if (findAndSetTargetRecordForAssertion()) {
                            matchRecordFound = true;
                            break;
                        }
                    }
                    if (matchRecordFound) {
                        System.out.println("Record consumed in attempt " + attempt + " are " + pollRecords.count()
                                + " and Record found with unique identifier.");
                        System.out.println("Details of record found with unique idetifier are as follows : ");
                        System.out.println("Key : " + kafkaConsumerPollRecord.get(key).key());
                        System.out.println("Partition : " + kafkaConsumerPollRecord.get(key).partition());
                        System.out.println("Offset : " + kafkaConsumerPollRecord.get(key).offset());
                        System.out.println("Value : " + kafkaConsumerPollRecord.get(key).value());
                        return pollRecords;
                    } else {
                        System.out.println("Record consumed in attempt " + attempt + " are " + pollRecords.count()
                                + ". But, no Record found with unique identifier.");
                    }
                    attempt++;
                } else {
                    System.out.println("Record consumed in attempt " + attempt + " are " + pollRecords.count() + ".");
                    attempt++;
                }

            } catch (Exception e) {
                System.out.println("Error in polling records : " + e.getMessage());
                attempt++;
            }
        }
        return null;
    }

    @Action(object = ObjectType.KAFKA, desc = "Identify target message", input = InputType.YES, condition = InputType.YES)
    public void identifyTargetMessage() {
        try {
            kafkaRecordIdentifierValue.put(key, Data);
            kafkaRecordIdentifierPath.put(key, Condition);
            Report.updateTestLog(Action,
                    "Target message set with tag value as [" + Data + "] and tag path as [" + Condition + "].",
                    Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error in target message setup : " + e.getMessage(), Status.FAIL);
        }
    }

    public boolean findAndSetTargetRecordForAssertion() { // identifyTargetMessage
        boolean matchFound = false;
        try {
            if (kafkaConsumerPollRecord.get(key).value() != null) {
                String recordValue = kafkaConsumerPollRecord.get(key).value().toString();
                boolean isJson = Pattern.matches("^\\s*(\\{.*\\}|\\[.*\\])\\s*$", recordValue);
                boolean isXml = Pattern.matches("^\\s*<\\?*xml*.*>.*<.*>.*</.*>\\s*$", recordValue);

                if (isJson) {
                    if (getJSONRecordForAssertion(recordValue)) {
                        matchFound = true;
                    }

                } else if (isXml) {
                    if (getXMLRecordForAssertion(recordValue)) {
                        matchFound = true;
                    }
                } else {
                    System.out.println("Unknown format");
                }
            }
        } catch (Exception e) {
            System.out.println("Error in find and set target record for assertion : " + e.getMessage());
        }
        return matchFound;
    }

    public boolean getJSONRecordForAssertion(String JSONMessage) {
        try {
            String jsonpath = kafkaRecordIdentifierPath.get(key);
            String value = JsonPath.read(JSONMessage, jsonpath).toString();
            if (value.equals(kafkaRecordIdentifierValue.get(key))) {
                kafkaConsumeRecordValue.put(key, JSONMessage);
                return true;
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
        return false;
    }

    public boolean getXMLRecordForAssertion(String XMLMessage) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(XMLMessage));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = kafkaRecordIdentifierPath.get(key);
            String value = (String) xPath.compile(expression).evaluate(doc);
            if (value.equals(kafkaRecordIdentifierValue.get(key))) {
                kafkaConsumeRecordValue.put(key, XMLMessage);
                return true;
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
        return false;
    }

    @Action(object = ObjectType.KAFKA, desc = "Close Consumer", input = InputType.NO, condition = InputType.NO)
    public void closeConsumer() {
        try {
            kafkaConsumerRecords.remove(key);
            kafkaConsumerRecord.remove(key);
            kafkaConsumeRecordValue.remove(key);
            kafkaConsumerPollDuration.remove(key);
            kafkaConsumerPollRetries.remove(key);
            kafkaConsumerTopic.remove(key);
            kafkaValueDeserializer.remove(key);
            kafkaSchemaRegistryURL.remove(key);
            kafkaSharedSecret.remove(key);
            kafkaConsumerGroupId.remove(key);
            kafkaConsumerPollRecord.remove(key);
            kafkaRecordIdentifierValue.remove(key);
            kafkaRecordIdentifierPath.remove(key);
            Report.updateTestLog(Action, "Consumer closed successfully", Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error in closing Consumer.", Status.DEBUG);
        }

    }

    @Action(object = ObjectType.KAFKA, desc = "Store XML tag In DataSheet ", input = InputType.YES, condition = InputType.NO)
    public void storeKafkaXMLtagInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String xmlText = kafkaConsumeRecordValue.get(key);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder;
                    InputSource inputSource = new InputSource();
                    inputSource.setCharacterStream(new StringReader(xmlText));
                    dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(inputSource);
                    doc.getDocumentElement().normalize();
                    XPath xPath = XPathFactory.newInstance().newXPath();
                    String expression = Condition;
                    String value = (String) xPath.compile(expression).evaluate(doc);
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(Action, "Element text [" + value + "] is stored in " + strObj, Status.DONE);
                } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                        | SAXException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing XML element in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing XML element in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Assert XML Tag Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertKafkaXMLtagEquals() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(kafkaConsumeRecordValue.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = Condition;
            String value = (String) xPath.compile(expression).evaluate(doc);
            if (value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] is not as expected", Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Assert XML Tag Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertKafkaXMLtagContains() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(kafkaConsumeRecordValue.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = Condition;
            String value = (String) xPath.compile(expression).evaluate(doc);
            if (value.contains(Data)) {
                Report.updateTestLog(Action, "Element text contains [" + Data + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Assert Response Message contains ", input = InputType.YES)
    public void assertKafkaResponseMessageContains() {
        try {
            if (kafkaConsumeRecordValue.get(key).contains(Data)) {
                Report.updateTestLog(Action, "Response Message contains : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Response Message does not contain : " + Data, Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response body :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Assert JSON Tag Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertKafkaJSONtagEquals() {
        try {
            String response = kafkaConsumeRecordValue.get(key);
            String jsonpath = Condition;
            String value = JsonPath.read(response, jsonpath).toString();
            if (value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text is [" + value + "] but is expected to be [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Assert JSON Tag Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertKafkaJSONtagContains() {
        try {
            String response = kafkaConsumeRecordValue.get(key);
            String jsonpath = Condition;
            String value = JsonPath.read(response, jsonpath).toString();
            if (value.contains(Data)) {
                Report.updateTestLog(Action, "Element text contains [" + Data + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Store JSON Tag In DataSheet ", input = InputType.YES, condition = InputType.YES)
    public void storeKafkaJSONtagInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String response = kafkaConsumeRecordValue.get(key);
                    String jsonpath = Condition;
                    String value = JsonPath.read(response, jsonpath).toString();
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(Action, "Element text [" + value + "] is stored in " + strObj, Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Store Response In DataSheet ", input = InputType.YES, condition = InputType.NO)
    public void storeKafkaResponseInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String response = kafkaConsumeRecordValue.get(key);
                    userData.putData(sheetName, columnName, response);
                    Report.updateTestLog(Action, "Response is stored in " + strObj, Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error storing Response in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error storing Response in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    // to add Configs in props
    public Properties addConfigProps(Properties props) {
        for (String config : kafkaConfigs.get(key)) {
            String[] keyValue = config.split("=", 2);
            if (keyValue.length == 2) {
                props.put(keyValue[0], keyValue[1]);
            }
        }
        return props;
    }

    public Properties getProducersslConfigurations(Properties prop) {
        Properties sslProp = Control.getCurrentProject().getProjectSettings().getKafkaSSLConfigurations();
        Set<String> keys = sslProp.stringPropertyNames();
        for (String key : keys) {
            String value = sslProp.getProperty(key);
            value = handleDataSheetVariables(value);
            value = handleuserDefinedVariables(value);
            switch (key) {
                case "Producer_ssl_Enabled":
                    Boolean.valueOf(value);
                    break;
                case "Producer_Truststore_Location":
                    String producertrustStroreLocation = Paths.get(value).toAbsolutePath().toString();
                    prop.put("ssl.truststore.location", producertrustStroreLocation);
                    break;
                case "Producer_Truststore_Password":
                    prop.put("ssl.truststore.password", value);
                    break;
                case "Producer_Keystore_Location":
                    String producerKeyStroreLocation = Paths.get(value).toAbsolutePath().toString();
                    prop.put("ssl.keystore.location", producerKeyStroreLocation);
                    break;
                case "Producer_Keystore_Password":
                    prop.put("ssl.keystore.password", value);
                    break;
                case "Producer_Key_Password":
                    prop.put("ssl.key.password", value);
                    break;
            }
        }
        return prop;
    }

    public Properties getConsumersslConfigurations(Properties prop) {
        Properties sslProp = Control.getCurrentProject().getProjectSettings().getKafkaSSLConfigurations();
        Set<String> keys = sslProp.stringPropertyNames();
        for (String key : keys) {
            String value = sslProp.getProperty(key);
            value = handleDataSheetVariables(value);
            value = handleuserDefinedVariables(value);
            switch (key) {
                case "Consumer_ssl_Enabled":
                    break;
                case "Consumer_Truststore_Location":
                    String trustStroreLocation = Paths.get(value).toAbsolutePath().toString();
                    prop.put("ssl.truststore.location", trustStroreLocation);
                    break;
                case "Consumer_Truststore_Password":
                    prop.put("ssl.truststore.password", value);
                    break;
                case "Consumer_Keystore_Location":
                    String consumerKeyStroreLocation = Paths.get(value).toAbsolutePath().toString();
                    prop.put("ssl.keystore.location", consumerKeyStroreLocation);
                    break;
                case "Consumer_Keystore_Password":
                    prop.put("ssl.keystore.password", value);
                    break;
                case "Consumer_Key_Password":
                    prop.put("ssl.key.password", value);
                    break;
            }
        }
        return prop;
    }

    public boolean isProducersslEnabled() {
        Properties prop = Control.getCurrentProject().getProjectSettings().getKafkaSSLConfigurations();
        String value = prop.getProperty("Producer_ssl_Enabled");
        value = handleRuntimeValues(value);
        return "true".equalsIgnoreCase(value);

    }

    public String handleRuntimeValues(String value) {
        value = handleDataSheetVariables(value);
        value = handleuserDefinedVariables(value);
        return value;
    }

    public boolean isConsumersslEnabled() {
        Properties prop = Control.getCurrentProject().getProjectSettings().getKafkaSSLConfigurations();
        String value = prop.getProperty("Consumer_ssl_Enabled");
        value = handleRuntimeValues(value);
        return "true".equalsIgnoreCase(value);
    }

    // Added to create avro compatible message
    public void getAvroCompatibleMessage() {
        String jsonAvroMessage = "";
        try {
            ObjectMapper stringMapper = new ObjectMapper();
            JsonNode inputJson = mapper.readTree(kafkaValue.get(key).toString());
//            JsonNode inputJson = stringMapper.readTree((String) kafkaValue.get(key));
            JsonNode avroCompatibleJson = convertNode(inputJson, kafkaAvroSchema.get(key));
            jsonAvroMessage = stringMapper.writerWithDefaultPrettyPrinter().writeValueAsString(avroCompatibleJson);
            kafkaAvroCompatibleMessage.put(key, jsonAvroMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                    mapNode.set(entry.getKey(), convertNode(entry.getValue(), schema.getValueType()));
                }
                return mapNode;

            case UNION:
                for (Schema subSchema : schema.getTypes()) {
                    if (subSchema.getType() == Schema.Type.NULL && (input == null || input.isNull())) {
                        return NullNode.getInstance();
                    }

                    if (isCompatible(input, subSchema)) {
                        JsonNode wrapped = convertNode(input, subSchema);
                        ObjectNode unionNode = mapper.createObjectNode();

                        // ✅ Use fully qualified name for ENUM and RECORD
                        String typeName = (subSchema.getType() == Schema.Type.RECORD
                                || subSchema.getType() == Schema.Type.ENUM) ? subSchema.getFullName()
                                : subSchema.getType().getName();

                        unionNode.set(typeName, wrapped);
                        return unionNode;
                    }
                }

                System.err.println("❌ No matching type in union for value: " + input);
                System.err.println("Schema: " + schema.toString(true));
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
}

*/
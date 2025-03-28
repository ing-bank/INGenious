package com.ing.engine.commands.kafka;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.jayway.jsonpath.JsonPath;
//import io.confluent.kafka.serializers.KafkaAvroDeserializer;
//import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
//import io.confluent.kafka.serializers.KafkaAvroSerializer;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
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

    @Action(object = ObjectType.KAFKA, desc = "Set Bootstrap Servers", input = InputType.YES, condition = InputType.NO)
    public void setBootstrapServers() {
        try {
            kafkaServers.put(key, Data);
            Report.updateTestLog(Action, "Bootstrap Servers have been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Bootstrap Servers setup", ex);
            Report.updateTestLog(Action, "Error in setting Bootstrap Servers: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Schema Registry URL", input = InputType.YES, condition = InputType.NO)
    public void setSchemaRegistryURL() {
        try {
            kafkaSchemaRegistryURL.put(key, Data);
            Report.updateTestLog(Action, "Schema Registry URL has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Schema Registry URL setup", ex);
            Report.updateTestLog(Action, "Error in setting Schema Registry URL: " + "\n" + ex.getMessage(), Status.DEBUG);
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
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Consumer GroupId setup", ex);
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
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Value Serializer setup", ex);
            Report.updateTestLog(Action, "Error in setting Value Serializer: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Set Value Deserializer", input = InputType.YES, condition = InputType.NO)
    public void setValueDeserializer() {
        try {
            kafkaValueDeserializer.put(key, Data);
            Report.updateTestLog(Action, "Value Deserializer has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Value Deserializer setup", ex);
            Report.updateTestLog(Action, "Error in setting Value Deserializer: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Produce Kafka Message", input = InputType.YES, condition = InputType.NO)
    public void produceMessage() {
        try {
            String value = Data;
            value = handleDataSheetVariables(value);
            value = handleuserDefinedVariables(value);
            kafkaValue.put(key, value);

            if (kafkaHeaders.get(key) != null && kafkaTimeStamp.get(key) != null) {
                produceMessage(kafkaProducerTopic.get(key), kafkaPartition.get(key), kafkaTimeStamp.get(key), kafkaKey.get(key), kafkaValue.get(key), kafkaHeaders.get(key));
            } else if (kafkaHeaders.get(key) != null) {
                produceMessage(kafkaProducerTopic.get(key), kafkaPartition.get(key), kafkaKey.get(key), kafkaValue.get(key), kafkaHeaders.get(key));
            } else if (kafkaTimeStamp.get(key) != null) {
                produceMessage(kafkaProducerTopic.get(key), kafkaPartition.get(key), kafkaTimeStamp.get(key), kafkaKey.get(key), kafkaValue.get(key));
            } else if (kafkaPartition.containsKey(key)) {
                produceMessage(kafkaProducerTopic.get(key), kafkaPartition.get(key), kafkaKey.get(key), kafkaValue.get(key));
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

    @Action(object = ObjectType.KAFKA, desc = "Send Message", input = InputType.NO, condition = InputType.NO)
    public void sendKafkaMessage() {
        try {
            createProducer(kafkaValueSerializer.get(key));
            //before.put(key, Instant.now());
            kafkaProducer.get(key).send(kafkaProducerRecord.get(key));
            Report.updateTestLog(Action, "Record sent", Status.DONE);
            kafkaProducer.get(key).close();
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception while sending record", ex);
            Report.updateTestLog(Action, "Error in sending record: " + "\n" + ex.getMessage(), Status.DEBUG);
        } finally {
            clearProducerDetails();
        }
    }

    private void createProducer(String serializer) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers.get(key));

        if (serializer.toLowerCase().contains("string")) {
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        } else if (serializer.toLowerCase().contains("bytearray")) {
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        } else if (serializer.toLowerCase().contains("avro")) {
           // props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
           // props.put("schema.registry.url", kafkaSchemaRegistryURL.get(key));
        } else {
            throw new IllegalArgumentException("Unsupported value type");
        }

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer.put(key, new KafkaProducer<>(props));
    }

    private void produceMessage(String topic, String value) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, value));
    }

    private void produceMessage(String topic, String kafkaKey, String value) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, kafkaKey, value));
    }

    private void produceMessage(String topic, Integer partition, String kafkaKey, String value) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, partition, kafkaKey, value));
    }

    private void produceMessage(String topic, Integer partition, long timestamp, String kafkaKey, String value) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, partition, timestamp, kafkaKey, value));
    }

    private void produceMessage(String topic, Integer partition, String kafkaKey, String value, List<Header> headers) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, partition, kafkaKey, value, headers));
    }

    private void produceMessage(String topic, Integer partition, long timestamp, String kafkaKey, String value, List<Header> headers) {
        kafkaProducerRecord.put(key, new ProducerRecord<>(topic, partition, timestamp, kafkaKey, value, headers));
    }

    private String handleDataSheetVariables(String payloadstring) {
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (payloadstring.contains("{" + sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject()
                        .getTestData().getTestDataByName(sheetlist.get(sheet));
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
    }

    public void createConsumer(String deserializer) {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers.get(key));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerGroupId.get(key));

        if (deserializer.toLowerCase().contains("string")) {
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        } else if (deserializer.toLowerCase().contains("bytearray")) {
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        } else if (deserializer.toLowerCase().contains("avro")) {
           // props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
           // props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
           // props.put("schema.registry.url", kafkaSchemaRegistryURL.get(key));
        } else {
            throw new IllegalArgumentException("Unsupported value type");
        }

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaConsumer.put(key, new KafkaConsumer<>(props));
    }

    @Action(object = ObjectType.KAFKA, desc = "Consume Kafka Message", input = InputType.NO)
    public void consumeKafkaMessage() {

        createConsumer(kafkaValueDeserializer.get(key));
        kafkaConsumer.get(key).subscribe(Arrays.asList(kafkaConsumerTopic.get(key)));
        try {
            ConsumerRecord record = pollKafkaConsumer();
            if (record != null) {
                Report.updateTestLog(Action, "Kafka message consumed successfully. ", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Kafka message not received. ", Status.FAIL);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Report.updateTestLog(Action, "Error while consuming Kafka message: " + e.getMessage(), Status.FAIL);
        } finally {
            kafkaConsumer.get(key).close();
        }
    }

    private ConsumerRecord<String, Object> pollKafkaConsumer() {
        int maxRetries = kafkaConsumerPollRetries.get(key);
        int attempt = 0;

        while (attempt < maxRetries) {
            ConsumerRecords<String, Object> records = kafkaConsumer.get(key).poll(Duration.ofMillis(kafkaConsumerPollDuration.get(key)));
            if (!records.isEmpty()) {
                kafkaConsumerRecord.put(key, records.iterator().next());
                return kafkaConsumerRecord.get(key);
            }
            attempt++;
        }
        return null;
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
                    String xmlText = kafkaConsumerRecord.get(key).value().toString();
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
            inputSource.setCharacterStream(new StringReader(kafkaConsumerRecord.get(key).value().toString()));
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
            inputSource.setCharacterStream(new StringReader(kafkaConsumerRecord.get(key).value().toString()));
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
            if (kafkaConsumerRecord.get(key).value().toString().contains(Data)) {
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
            String response = kafkaConsumerRecord.get(key).value().toString();
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
            String response = kafkaConsumerRecord.get(key).value().toString();
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
                    String response = kafkaConsumerRecord.get(key).value().toString();
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
                    String response = kafkaConsumerRecord.get(key).value().toString();
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

}

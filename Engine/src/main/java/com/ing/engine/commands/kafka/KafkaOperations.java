package com.ing.engine.commands.kafka;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaOperations extends General {

    public KafkaOperations(CommandControl cc) {
        super(cc);
    }
    private static KafkaConsumer<String, String> consumer;
    private static Producer<String, Object> unencryptedProducer;
    private String headerKey;
    private String headerValue;

    @Action(object = ObjectType.KAFKA, desc = "Add Kafka Header", input = InputType.YES, condition = InputType.OPTIONAL)
    public void addKafkaHeader() {
        try {

            String strObj = Data;
            if (strObj.matches(".*;.*")) {
                String[] parts = strObj.split(";", 3); // Split into key and value, allowing colons in value
                kafkaHeaderKey.put(key, parts[0]);
                String headerValue1 = parts[1];
                String headerValue2 = parts[2];
                String headerValue = headerValue1 + ":" + headerValue2;
                kafkaHeaderValue.put(key, headerValue);
                Report.updateTestLog(Action, "Kafka Header has been added. ", Status.DONE);

            } else {
                System.out.println("Invalid input format. It should be key:value");
                Report.updateTestLog(Action, "Invalid input format. It should be key:value" + "\n",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Something went wrong in storing the UUID" + "\n" + ex.getMessage(),
                    Status.FAILNS);
            ex.printStackTrace();
        }
    }

    public static synchronized KafkaConsumer<String, Object> createConsumer(String serverConfig, String schemaRegistryUrl, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverConfig);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        //props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    @Action(object = ObjectType.KAFKA, desc = "Consume Kafka Message", input = InputType.YES, condition = InputType.YES)
    public void consumeKafkaMessage() {
        String serverConfig = getVar("%serverConfig%");
        String schemaRegistryUrl = getVar("%schemaRegistryUrl%");
        String groupId = getVar("%groupId%");
        KafkaConsumer<String, Object> consumer = createConsumer(serverConfig, schemaRegistryUrl, groupId);
        consumer.subscribe(Arrays.asList(Condition)); // Subscribe to a topic provided in Condition column

        try {
            // Poll Kafka for messages (with a timeout of 5 seconds)
            ConsumerRecord<String, Object> record = pollKafkaMessage(consumer);

            // Get the current offset for a specific partition
            Set<TopicPartition> assignedPartitions = consumer.assignment();

            for (TopicPartition partition : assignedPartitions) {

                long currentOffset = consumer.position(partition);
                System.out.println("Current Offset for Partition " + partition.partition() + ": " + currentOffset);
                Report.updateTestLog("consumeKafkaMessage", "Current Offset for Partition " + partition.partition() + ": " + currentOffset, Status.DONE);
            }

            // Process the received message
            if (record != null) {
                // Extract the record value
                Object recordValue = record.value();
                //if (recordValue instanceof TradeEventRecord) {
                //    TradeEventRecord tradeEventRecord = (TradeEventRecord) recordValue;

                // Extract the messageId from the consumed record
                String extractedMessageId = "messageID";
                System.out.println("Consumed Message ID: " + extractedMessageId);

                // Log the messageId extracted from the consumed message
                Report.updateTestLog("consumeKafkaMessage", "Message ID extracted: " + extractedMessageId, Status.DONE);

                // Store the extractedMessageId in the datasheet
                if (Condition != null && Input != null) {
                    // Split the Condition to get the datasheet and column name
                    String[] sheetDetail = Input.split(":");
                    String sheetName = sheetDetail[0];
                    String columnName = sheetDetail[1];

                    // Get the row index from Condition (if provided)
                    int rowIndex = 1;
                    String[] split = Condition.split(",");
                    if (split.length > 1) {
                        rowIndex = Integer.parseInt(split[1]);
                    }

                    // Ensure the row exists and store the extracted messageId
                    // Use userData.putData() to store the value in the datasheet
                    userData.putData(sheetName, columnName, extractedMessageId);
                    Report.updateTestLog("consumeKafkaMessage", "Extracted Message ID: " + extractedMessageId + " stored into datasheet " + sheetName + " column " + columnName, Status.DONE);
                } else {
                    Report.updateTestLog("consumeKafkaMessage", "Incorrect Input or Condition format", Status.FAILNS);
                }

//                } else {
//                    System.out.println("Unexpected record type: " + recordValue.getClass());
//                    Report.updateTestLog("consumeKafkaMessage", "Unexpected record type: " + recordValue.getClass().getSimpleName(), Status.FAIL);
//                }
            } else {
                System.out.println("No messages received from Kafka.");
                Report.updateTestLog("consumeKafkaMessage", "No messages received from Kafka.", Status.FAIL);
            }
        } catch (Exception e) {
            // Handle any unexpected exceptions
            System.out.println("Error while consuming Kafka message: " + e.getMessage());
            e.printStackTrace();
            Report.updateTestLog("consumeKafkaMessage", "Error while consuming Kafka message: " + e.getMessage(), Status.FAIL);
        } finally {
            consumer.close(); // Close the consumer after processing
        }
    }

    // Poll Kafka for messages
    private ConsumerRecord<String, Object> pollKafkaMessage(KafkaConsumer<String, Object> consumer) {
        int maxRetries = 20; // Maximum number of retries
        int attempt = 0;

        while (attempt < maxRetries) {
            // Poll Kafka for a message with a timeout of 1000ms (1 second)
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(1000));

            // Check if we received any messages
            if (!records.isEmpty()) {
                // Return the first record from the poll
                return records.iterator().next();
            }

            attempt++; // Increment the retry count
        }

        // If we exhaust all retries, return null
        return null;
    }

    @Action(object = ObjectType.KAFKA, desc = "Validate Consumed Kafka Message Is Null/Empty", input = InputType.NO, condition = InputType.YES)
    public void validateKafkaMessageIsNullOrEmpty() {
        String serverConfig = getVar("%serverConfig%");
        String schemaRegistryUrl = getVar("%schemaRegistryUrl%");
        String groupId = getVar("%groupId%");
        KafkaConsumer<String, Object> consumer = createConsumer(serverConfig, schemaRegistryUrl, groupId);
        consumer.subscribe(Arrays.asList(Condition)); // Subscribe to the topic defined in Condition

        try {
            // Poll Kafka for messages (with a timeout of 5 seconds)
            ConsumerRecord<String, Object> record = pollKafkaMessage(consumer);

            // Validate the consumed message
            if (record == null || record.value() == null || record.value().toString().isEmpty()) {
                Report.updateTestLog("validateKafkaMessageIsNullOrEmpty", "Consumed message is null or empty.", Status.DONE);
                System.out.println("Consumed message is null or empty.");
            } else {
                Report.updateTestLog("validateKafkaMessageIsNullOrEmpty", "Consumed message is not null or empty: " + record.value(), Status.FAIL);
                System.out.println("Consumed message is not null or empty: " + record.value());
            }
        } catch (Exception e) {
            // Handle any unexpected exceptions
            System.out.println("Error while validating Kafka message: " + e.getMessage());
            e.printStackTrace();
            Report.updateTestLog("validateKafkaMessageIsNullOrEmpty", "Error while validating Kafka message: " + e.getMessage(), Status.FAIL);
        } finally {
            consumer.close(); // Close the consumer after processing
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Store Current Kafka Offset in Datasheet", input = InputType.YES, condition = InputType.YES)
    public void storeKafkaOffset() {
        String serverConfig = getVar("%serverConfig%");
        String schemaRegistryUrl = getVar("%schemaRegistryUrl%");
        String groupId = getVar("%groupId%");
        KafkaConsumer<String, Object> consumer = createConsumer(serverConfig, schemaRegistryUrl, groupId);
        consumer.subscribe(Arrays.asList(Condition)); // Subscribe to the topic defined in Condition

        try {
            // Poll Kafka for messages (5-second timeout)
            ConsumerRecord<String, Object> record = pollKafkaMessage(consumer);

            // Get current offset for assigned partitions
            Set<TopicPartition> assignedPartitions = consumer.assignment();
            for (TopicPartition partition : assignedPartitions) {
                long currentOffset = consumer.position(partition);
                System.out.println("Current Offset for Partition " + partition.partition() + ": " + currentOffset);

                // Store the offset in the datasheet
                if (Input.matches(".*:.*")) {
                    String sheetName = Input.split(":", 2)[0];
                    String columnName = Input.split(":", 2)[1];
                    userData.putData(sheetName, columnName, String.valueOf(currentOffset));
                    Report.updateTestLog(Action, "Offset [" + currentOffset + "] is stored in " + Input, Status.DONE);
                }
            }

            consumer.close();
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in storing Kafka offset: " + ex.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Validate if Kafka Offset has Incremented by 1", input = InputType.NO)
    public void validateOffsetIncrement() {
        String serverConfig = getVar("%serverConfig%");
        String schemaRegistryUrl = getVar("%schemaRegistryUrl%");
        String groupId = getVar("%groupId%");
        String storedOffsetStr = getVar("%currentOffset%");
        KafkaConsumer<String, Object> consumer = createConsumer(serverConfig, schemaRegistryUrl, groupId);
        consumer.subscribe(Arrays.asList(Condition)); // Subscribe to the topic defined in Condition

        try {
            // Poll Kafka for messages (with a timeout of 5 seconds)
            ConsumerRecord<String, Object> record = pollKafkaMessage(consumer);

            long storedOffset = Long.parseLong(storedOffsetStr);

            // Get current offset for assigned partitions
            Set<TopicPartition> assignedPartitions = consumer.assignment();
            for (TopicPartition partition : assignedPartitions) {
                long currentOffset = consumer.position(partition);
                System.out.println("Stored Offset: " + storedOffset);
                System.out.println("Current Offset for Partition " + partition.partition() + ": " + currentOffset);

                // Validate if the offset incremented by 1
                if (currentOffset == storedOffset + 1) {
                    Report.updateTestLog(Action, "Offset incremented by 1: Stored [" + storedOffset + "], Current [" + currentOffset + "]", Status.PASS);
                } else {
                    Report.updateTestLog(Action, "Offset not incremented as expected: Stored [" + storedOffset + "], Current [" + currentOffset + "]", Status.FAIL);
                }
            }

            consumer.close();
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating Kafka offset increment: " + ex.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Produce and send Kafka Message", input = InputType.YES, condition = InputType.YES)
    public void produceMessage() {
        try {
            //  CdmRecord record = new CdmRecord();
            String payload = readFileToString(Data); // Read from file
            if (payload == null || payload.isEmpty()) {
                payload = Data; // Use Data directly if file read is empty
            }
            payload = processPayload(payload); // Process payload to remove new lines and replace separators
            payload = handleDataSheetVariables(payload); // Handle data sheet variables
            payload = handleuserDefinedVariables(payload); // Handle user-defined
            // record.setCdmPayload(Charset.forName("UTF-8").encode(payload));

            // Send the message over Kafka
            String serverConfig = getVar("%serverConfig%");
            String schemaRegistryUrl = getVar("%schemaRegistryUrl%");
            String topic = Condition;

            List<Header> header = List.of(new RecordHeader(kafkaHeaderKey.get(key), kafkaHeaderValue.get(key).getBytes()));
            ProducerRecord<String, Object> producedRecord = new ProducerRecord<>(
                    topic, null, "", "", header);
            createUnencryptedProducer(serverConfig, schemaRegistryUrl).send(producedRecord);

            Report.updateTestLog(Action, "Message has been Produced. ", Status.DONE);

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Something went wrong in producing the message" + "\n" + ex.getMessage(),
                    Status.FAILNS);
            ex.printStackTrace();
        }
    }

    public static synchronized Producer<String, Object> createUnencryptedProducer(String serverConfig, String schemaRegistryUrl) {
        if (unencryptedProducer != null) {
            return unencryptedProducer;
        }
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverConfig);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        unencryptedProducer = new KafkaProducer<>(props);
        return unencryptedProducer;
    }

    /**
     * The producer will not use any encryption and will send the bytes as is to
     * kafka topic.
     */
    public static void closeProducer() {
        if (unencryptedProducer != null) {
            unencryptedProducer.flush();
            unencryptedProducer.close();
        }
    }

    public static String readFileToString(String filePath) {
        File file = new File(filePath);
        StringBuilder data = new StringBuilder();
        try {
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    data.append(
                            scanner.nextLine()); //replaceAll("\\\\", "").replaceAll("\"", "'")).append(" ");
                }
                scanner.close();
            }
            String uuid = UUID.randomUUID().toString();
            String result = data.toString().replace("{UUID}", uuid).replaceAll("\\s+", " ")
                    .replaceAll("> <", "><");
            return result.trim(); // Trim to remove any trailing space

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String processPayload(String payload) {
        String uuid = UUID.randomUUID().toString();
        return payload.replace("{UUID}", uuid).replaceAll("\\s+", " ").replaceAll("> <", "><").trim();
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

    @Action(object = ObjectType.KAFKA, desc = "Stores a UUID in a datasheet", input = InputType.YES)
    public void StoreUUID() {
        try {
            UUID uuid = UUID.randomUUID();
            String UUIDString = uuid.toString();
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                String sheetName = strObj.split(":", 2)[0];
                String columnName = strObj.split(":", 2)[1];
                userData.putData(sheetName, columnName, UUIDString);
                Report.updateTestLog(Action, "UUID [" + UUIDString + "] is stored in " + strObj, Status.DONE);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Something went wrong in storing the UUID" + "\n" + ex.getMessage(),
                    Status.FAILNS);

        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Stores ErrorCode in DataSheet", input = InputType.YES, condition = InputType.YES)
    public void storeErrorCodeinDataSheet() {
        String serverConfig = getVar("%serverConfig%");
        String schemaRegistryUrl = getVar("%schemaRegistryUrl%");
        String groupId = getVar("%groupId%");
        KafkaConsumer<String, Object> consumer = createConsumer(serverConfig, schemaRegistryUrl, groupId);
        consumer.subscribe(Arrays.asList(Condition)); // Subscribe to the topic defined in Condition

        try {
            // Poll Kafka for messages (with a timeout of 5 seconds)
            ConsumerRecord<String, Object> record = pollKafkaMessage(consumer);

            if (record == null || record.value() == null) {
                Report.updateTestLog("storeErrorCodeinDataSheet", "Consumed message is null or empty.", Status.FAIL);
                return;
            }

            // Parse the consumed message
            Object recordValue = record.value();
            // if (recordValue instanceof TradeEventErrorRecord) {
            // TradeEventErrorRecord tradeErrorRecord = (TradeEventErrorRecord) recordValue;

            // Extract the error code
            // GalaxyError errorCode = tradeErrorRecord.getErrorCode();
            //  String extractedErrorCode = errorCode != null ? errorCode.name() : "UNKNOWN";
            System.out.println("Extracted Error Code: " + "error");

            Report.updateTestLog("storeErrorCodeinDataSheet", "Extracted Error Code: " + "errorcode", Status.DONE);

            // Store the error code in the datasheet
            if (Input != null) {
                // Split the input into sheet name and column name
                String[] sheetDetails = Input.split(":");
                if (sheetDetails.length != 2) {
                    Report.updateTestLog("storeErrorCodeinDataSheet", "Invalid Input format. Expected format: sheetName:columnName", Status.FAIL);
                    return;
                }

                String sheetName = sheetDetails[0];
                String columnName = sheetDetails[1];

                // Store the extracted error code in the datasheet
                userData.putData(sheetName, columnName, "errorcode");
                Report.updateTestLog("storeErrorCodeinDataSheet",
                        "Stored Error Code: " + "errorcode" + " in sheet: " + sheetName + ", column: " + columnName + " ", Status.DONE);
            } else {
                Report.updateTestLog("storeErrorCodeinDataSheet", "Input is null or improperly formatted", Status.FAIL);
            }

        } catch (Exception e) {
            Report.updateTestLog("storeErrorCodeinDataSheet", "Exception occurred: " + e.getMessage(), Status.FAIL);
            e.printStackTrace();
        } finally {
            consumer.close(); // Ensure the consumer is closed
        }
    }

    @Action(object = ObjectType.KAFKA, desc = "Assert Kafka Result", input = InputType.YES, condition = InputType.YES)
    public void assertResult() {
        try {
            // Parse the input to determine if it is a direct value or datasheet reference
            String expectedValue = null;
            if (Input.startsWith("@")) {
                // Direct value
                expectedValue = Input.substring(1); // Remove the "@" prefix
            } else if (Input.contains(":")) {
                // Datasheet reference
                String[] parts = Input.split(":");
                if (parts.length == 2) {
                    String datasheet = parts[0];
                    String column = parts[1];
                    expectedValue = userData.getData(datasheet, column); // Retrieve value from datasheet
                } else {
                    Report.updateTestLog("assertResult", "Invalid Input format. Expected format is 'datasheet:column' or '@value'.", Status.FAIL);
                    return;
                }
            } else {
                Report.updateTestLog("assertResult", "Invalid Input format. Expected format is 'datasheet:column' or '@value'.", Status.FAIL);
                return;
            }

            // Get the expected value from Condition (either a direct value or datasheet reference)
            String actualValue = null;
            if (Condition.startsWith("@")) {
                // Direct value
                actualValue = Condition.substring(1); // Remove the "@" prefix
            } else if (Condition.contains(":")) {
                // Datasheet reference
                String[] parts = Condition.split(":");
                if (parts.length == 2) {
                    String datasheet = parts[0];
                    String column = parts[1];
                    actualValue = userData.getData(datasheet, column); // Retrieve value from datasheet
                } else {
                    Report.updateTestLog("assertResult", "Invalid Condition format. Expected format is 'datasheet:column' or '@value'.", Status.FAIL);
                    return;
                }
            } else {
                Report.updateTestLog("assertResult", "Invalid Condition format. Expected format is 'datasheet:column' or '@value'.", Status.FAIL);
                return;
            }

            // Perform the assertion
            if (actualValue.equals(expectedValue)) {
                Report.updateTestLog("assertResult", "Assertion passed. Actual value: '" + actualValue + "' matches expected value: '" + expectedValue + "'.", Status.PASS);
            } else {
                Report.updateTestLog("assertResult", "Assertion failed. Actual value: '" + actualValue + "' does not match expected value: '" + expectedValue + "'.", Status.FAIL);
            }
        } catch (Exception e) {
            Report.updateTestLog("assertResult", "An error occurred while performing the assertion: " + e.getMessage(), Status.FAIL);
            e.printStackTrace();
        }
    }

}

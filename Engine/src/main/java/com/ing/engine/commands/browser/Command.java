package com.ing.engine.commands.browser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.image.ImageORObject;
import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.AutomationObject;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.reporting.TestCaseReport;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.drivers.MobileObject;
import java.io.File;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.apache.kafka.common.header.Header;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class Command {

    public Page Page;
    public Playwright Playwright;
    public BrowserContext BrowserContext;
    public AutomationObject AObject;
    public MobileObject MObject;
    public PlaywrightDriverCreation Driver;
    public String Data;
    public String ObjectName;
    public Locator Locator;
    public ObjectGroup<ImageORObject> imageObjectGroup;
    public String Description;
    public String Condition;
    public String Input;
    public String Action;
    public TestCaseReport Report;
    public String Reference;
    private final CommandControl Commander;
    public UserDataAccess userData;
    public WebDriver mDriver;
    public WebElement Element;
    public MobileObject mObject;

    /**
     * ******API*******
     */
    static public Map<String, String> endPoints = new HashMap<>();
    static public Map<String, ArrayList<String>> headers = new HashMap<>();
    static public Map<String, ArrayList<String>> urlParams = new HashMap<>();
    static public Map<String, String> responsebodies = new HashMap<>();
    static public Map<String, String> responsecodes = new HashMap<>();
    static public Map<String, String> responsemessages = new HashMap<>();
    static public Map<String, APIRequestContext> requests = new HashMap<>();
    static public Map<String, APIResponse> responses = new HashMap<>();
    static public Map<String, java.net.http.HttpRequest.Builder> httpRequestBuilder = new HashMap<>();
    static public Map<String, java.net.http.HttpRequest> httpRequest = new HashMap<>();
    static public Map<String, java.net.http.HttpClient.Builder> httpClientBuilder = new HashMap<>();
    static public Map<String, java.net.http.HttpClient> httpClient = new HashMap<>();
    static public Map<String, java.net.http.HttpResponse> response = new HashMap<>();
    static public Map<String, String> httpagents = new HashMap<>();
    static public Map<String, Instant> before = new HashMap<>();
    static public Map<String, Instant> after = new HashMap<>();
    static public Map<String, Long> duration = new HashMap<>();
    public String key;
    public String scenarioContext;
    public String testCaseContext;
    public String iterationContext;
    public String subIterationContext;
    static public String basicAuthorization;
    /**
     * ************************
     */

    /**
     * Playwright Mocking *
     */
    static public Map<String, String> mockEndPoints = new HashMap<>();

    /**
     * ************************
     */
    
    /**
     * Data faker *
     */
    static public Map<String, Faker> faker = new HashMap<>();

    /**
     * ************************
     */
    
    /**
     * *** Queue ****
     */
    static public Map<String, String> jmsHost = new HashMap<>();
    static public Map<String, Integer> jmsPort = new HashMap<>();
    static public Map<String, String> jmsChannel = new HashMap<>();
    static public Map<String, String> jmsQmgr = new HashMap<>();
    static public Map<String, String> jmsUsername = new HashMap<>();
    static public Map<String, String> jmsPassword = new HashMap<>();
    static public Map<String, String> WMQ_SSL_CIPHER_SUITE = new HashMap<>();
    static public Map<String, String> jmsReqQueueName = new HashMap<>();
    static public Map<String, String> jmsRespQueueName = new HashMap<>();
    static public Map<String, JMSContext> jmsContext = new HashMap<>();
    static public Map<String, Destination> jmsDestination = new HashMap<>();
    static public Map<String, JMSProducer> jmsProducer = new HashMap<>();
    static public Map<String, JMSConsumer> jmsConsumer = new HashMap<>();
    static public Map<String, JmsFactoryFactory> jmsFactoryFactory = new HashMap<>();
    static public Map<String, JmsConnectionFactory> jmsConnectionFactory = new HashMap<>();
    static public Map<String, TextMessage> jmsMessage = new HashMap<>();
    static public Map<String, String> jmsCorrelationID = new HashMap<>();
    static public Map<String, String> receivedMessage = new HashMap<>();
    /**
     * **********
     */

    /**
     * *** Kafka Parameters ****
     */
    static public Map<String, List<Header>> kafkaHeaders = new HashMap<>();
    static public Map<String, String> kafkaProducerTopic = new HashMap<>();
    static public Map<String, String> kafkaConsumerTopic = new HashMap<>();
    static public Map<String, String> kafkaConsumerGroupId = new HashMap<>();
    static public Map<String, String> kafkaServers = new HashMap<>();
    static public Map<String, String> kafkaSchemaRegistryURL = new HashMap<>();
    static public Map<String, Integer> kafkaPartition = new HashMap<>();
    static public Map<String, Long> kafkaTimeStamp = new HashMap<>();
    static public Map<String, String> kafkaKey = new HashMap<>();
    static public Map<String, String> kafkaKeySerializer = new HashMap<>();
    static public Map<String, String> kafkaKeyDeserializer = new HashMap<>();
    static public Map<String, Object> kafkaValue = new HashMap<>();
    static public Map<String, String> kafkaValueSerializer = new HashMap<>();
    static public Map<String, String> kafkaValueDeserializer = new HashMap<>();
    static public Map<String, Integer> kafkaConsumerPollRetries = new HashMap<>();
    static public Map<String, Long> kafkaConsumerPollDuration = new HashMap<>();   
    static public Map<String, Schema> kafkaAvroSchema =new HashMap<>();
    static public Map<String, ProducerRecord<String, GenericRecord>> kafkaGenericRecord =new HashMap<>();
    static public Map<String, GenericRecord> kafkaGenericRecordValue =new HashMap<>();
    static public Map<String, KafkaProducer<String, GenericRecord>> kafkaAvroProducer =new HashMap<>();
    static public Map<String, ArrayList<String>> kafkaConfigs = new HashMap<>();
    static public Map<String, Properties> kafkaProducersslConfigs = new HashMap<>();
    static public Map<String, Properties> kafkaConsumersslConfigs = new HashMap<>();
    static public Map<String, String> kafkaAvroCompatibleMessage = new HashMap<>();
    static public Map<String, String> kafkaConsumeRecordCount = new HashMap<>();
    static public Map<String, String> kafkaConsumeRecordValue = new HashMap<>();
    static public Map<String, String> kafkaSharedSecret = new HashMap<>();
    static public Map<String, List<ConsumerRecord<String, Object>>> kafkaConsumerRecords = new HashMap<>();
    static public Map<String, ConsumerRecord<String, Object>> kafkaConsumerPollRecord = new HashMap<>();
    static public Map<String, String> kafkaRecordIdentifierValue = new HashMap<>();
    static public Map<String, String> kafkaRecordIdentifierPath = new HashMap<>();
    static public Map<String, Integer> kafkaConsumerMaxPollRecords = new HashMap<>();
    static public Map<String, Boolean> kafkaAutoRegisterSchemas = new HashMap<>();
    static public Map<String, ProducerRecord> kafkaProducerRecord = new HashMap<>();
    static public Map<String, ConsumerRecord> kafkaConsumerRecord = new HashMap<>();
    static public Map<String, KafkaProducer> kafkaProducer = new HashMap<>();
    static public Map<String, KafkaConsumer> kafkaConsumer = new HashMap<>();
    /**
     * **********
     */


    /**
     * ******SSH*******
     */
    // Regular SSH Session
    static public Map<String, String> sshHost = new HashMap<>();
    static public Map<String, Integer> sshHostPort = new HashMap<>();
    static public Map<String, String> sshHostUsername = new HashMap<>();
    static public Map<String, String> sshHostPassword = new HashMap<>();
    static public Map<String, Session> sshHostSession = new HashMap<>();
    static public Map<String, ChannelExec> sshHostChannelExec = new HashMap<>();
    static public Map<String, ChannelSftp> sshHostChannelSftp = new HashMap<>();
    static public Map<String, String> sshDestinationFolder = new HashMap<>();
    static public Map<String, String> sshCommandOutput = new HashMap<>();
    static public Map<String, String> regExCommandOutputResult = new HashMap<>();

    // SSH Session via Intermediary Session
    static public Map<String, String> sshIntermediaryHost = new HashMap<>();
    static public Map<String, Integer> sshIntermediaryHostPort = new HashMap<>();
    static public Map<String, String> sshIntermediaryHostUsername = new HashMap<>();
    static public Map<String, String> sshIntermediaryHostPassword = new HashMap<>();
    static public Map<String, Integer> sshIntermediaryHostTunnelPort = new HashMap<>();
    static public Map<String, Session> sshIntermediaryHostSession = new HashMap<>();
    /**
     * ************************
     */

    /**
     * ******Iban4j*******
     */
    static public Map<String, Iban> iban = new HashMap<>();

    static public Map<String, String> ibanAccountNumber = new HashMap<>();
    static public Map<String, String> ibanAccountType = new HashMap<>();
    static public Map<String, String> ibanBranchCode = new HashMap<>();
    static public Map<String, String> ibanBankCode = new HashMap<>();
    static public Map<String, String> ibanBankCodeExt = new HashMap<>();
    static public Map<String, String> ibanBban = new HashMap<>();
    static public Map<String, CountryCode> ibanCountryCode = new HashMap<>();
    static public Map<String, String> ibanIdentificationNumber = new HashMap<>();
    static public Map<String, Boolean> ibanLeftPadding = new HashMap<>();
    static public Map<String, String> ibanNationalCheckDigit = new HashMap<>();
    static public Map<String, String> ibanOwnerAccountType = new HashMap<>();
    /**
     * ************************
     */

    /**
     * ******File*******
     */
    static public Map<String, String> setFile = new HashMap<>();
    static public Map<String, List<String>> regexMatches = new HashMap<>();
    /**
     * ************************
     */

    /**
     * ******XML*******
     */
    // XML Operations
    static public Map<String, List<String>> ignoreXMLNodes = new HashMap<>();
    static public Map<String, List<String>> ignoreXMLTextNodes = new HashMap<>();

    // XML Builder
    static public Map<String, Document> document = new HashMap<>();
    static public Map<String, List<Element>> elementStack = new HashMap<>();
    static public Map<String, List<Boolean>> usedStack = new HashMap<>();
    static public Map<String, List<Element>> pendingStack = new HashMap<>();
    static public Map<String, List<Boolean>> pendingUsedStack = new HashMap<>();
    static public Map<String, String> lastAttributeName = new HashMap<>();
    /**
     * ************************
     */

    /**
     * ******JSON*******
     */
    // Json Operations
    static public Map<String, List<String>> ignoreJSONPaths = new HashMap<>();

    // Json Builder
    static public Map<String, JsonNodeFactory> factory = new HashMap<>();
    static public Map<String, ObjectMapper> mapper = new HashMap<>();
    static public Map<String, ObjectNode> rootNode = new HashMap<>();
    static public Map<String, List<ObjectNode>> objectStack = new HashMap<>();
    static public Map<String, List<Boolean>> jsonUsedStack = new HashMap<>();
    static public Map<String, List<String>> pendingKeys = new HashMap<>();
    static public Map<String, List<Boolean>> isArrayPending = new HashMap<>();
    static public Map<String, String> currentPropertyKey = new HashMap<>();
    static public Map<String, String> currentArrayKey = new HashMap<>();;
    /**
     * ************************
     */

    public Command(CommandControl cc) {
        Commander = cc;
        if (Commander.webDriver != null) {
            mDriver = Commander.webDriver.driver;
            mObject = Commander.MObject;
            Data = Commander.Data;
            ObjectName = Commander.ObjectName;
            Element = Commander.Element;
            imageObjectGroup = Commander.imageObjectGroup;
            Description = Commander.Description;
            Condition = Commander.Condition;
            Input = Commander.Input;
            Report = Commander.Report;
            Reference = Commander.Reference;
            Action = Commander.Action;
            userData = Commander.userData;
        } else {
            Page = Commander.Page.page;
            Playwright = Commander.Playwright.playwright;
            BrowserContext = Commander.BrowserContext.browserContext;
            AObject = Commander.AObject;
            Driver = Commander.Page;
            Data = Commander.Data;
            ObjectName = Commander.ObjectName;
            Locator = Commander.Locator;
            imageObjectGroup = Commander.imageObjectGroup;
            Description = Commander.Description;
            Condition = Commander.Condition;
            Input = Commander.Input;
            Report = Commander.Report;
            Reference = Commander.Reference;
            Action = Commander.Action;
            userData = Commander.userData;
        }


        /**
         * ******Webservice*******
         */
        key = userData.getScenario() + userData.getTestCase() + userData.getIteration();
        /**
         * ***********************
         */

        /**
         * ****** Print Context *******
         */
        scenarioContext = userData.getScenario();
        testCaseContext = userData.getScenario() + userData.getTestCase();
        iterationContext = userData.getScenario() + userData.getTestCase() + userData.getIteration();
        subIterationContext = userData.getScenario() + userData.getTestCase() + userData.getIteration() + userData.getSubIteration();

        /**
         * ***********************
         */
    }

    public void addVar(String key, String val) {
        Commander.addVar(key, val);
    }

    public String getRuntimeVar(String key) {
        return Commander.getRuntimeVar(key);
    }

    public String getVar(String key) {
        return Commander.getVar(key);
    }

    public String getVarWithoutWarning(String key) {
        return Commander.getVarWithoutWarning(key);
    }

    public void addGlobalVar(String key, String val) {
        if (key.matches("%.*%")) {
            key = key.substring(1, key.length() - 1);
        }
        Commander.putUserDefinedData(key, val);
    }

    public String getUserDefinedData(String key) {
        return Commander.getUserDefinedData(key);
    }

    public String getDatasheet(String key){
        return Commander.getDatasheet(key);
    }
    
    public Properties getDataBaseData(String val) {
        return Commander.getDataBaseProperty(val);
    }
    
    public File getDBFile(String val) {
        return new File(Commander.getDBFile(val));
    }

    public Stack<Locator> getRunTimeElement() {
        return Commander.getRunTimeElement();
    }

    public boolean shouldExecute(String condition) {
        // Run by default if condition is null or empty
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        if (condition.contains("=")) {
            String[] parts = condition.split("=");
            if (parts.length != 2) {
                System.out.println("Invalid condition format: " + condition);
                return false;
            }
            String key = parts[0].trim(); // e.g., %execution_origin%
            String[] settings = parts[1].split(",");
            if (settings.length == 0) {
                System.out.println("No settings provided in: " + condition);
                return false;
            }
            String userDefinedSetting = getVar(key);
            if (userDefinedSetting == null) {
                System.out.println("No value found for key: " + key);
                return false;
            }
            for (String setting : settings) {
                if (userDefinedSetting.contains(setting.trim())) {
                    return true;
                }
            }
            System.out.println("Splitting parts [" + key + "] provided 0 matches");
            return false;
        } else {
            return true;
        }
    }

    public void executeMethod(String Action) {
        Commander.executeAction(Action);
    }

    public void executeMethod(Locator Locator, String Action, String Input) {
        setElement(Locator);
        setInput(Input);
        executeMethod(Action);
    }

    public void executeMethod(String Action, String Input) {
        setInput(Input);
        executeMethod(Action);
    }

    public void executeMethod(Locator Locator, String Action) {
        setElement(Locator);
        executeMethod(Action);
    }

    public PlaywrightDriverCreation getDriverControl() {
        return Commander.Page;
    }

    public WebDriverCreation getMobileDriverControl() {
        return Commander.webDriver;
    }

    public Boolean isDriverAlive() {
        if (mDriver != null) {
            return getMobileDriverControl().isAlive();
        } else {
            return getDriverControl().isAlive();
        }
    }

    private void setElement(Locator Locator) {
        Commander.Locator = Locator;
    }

    private void setInput(String input) {
        Commander.Data = input;
    }

    public String getCurrentBrowserName() {
        return Commander.Page.getCurrentBrowser();
    }

    public CommandControl getCommander() {
        return Commander;
    }

    public void executeTestCase(String scenarioName, String testCaseName, int subIteration) {
        Commander.execute(scenarioName + ":" + testCaseName, subIteration);
    }

    public void executeTestCase(String scenarioName, String testCaseName) {
        executeTestCase(scenarioName, testCaseName, userData.getSubIterationAsNumber());
    }

    public boolean browserAction() {
        return "browser".equalsIgnoreCase(ObjectName);
    }
    
    public String resolveAllRuntimeVars(String str){
        return Commander.resolveAllRuntimeVars(str);
    }

    /**
     * ******Webservice**************
     */
    public String Endpoint() {
        return endPoints.get(key);
    }

    public String ResponseCode() {
        return responsecodes.get(key);
    }

    public String ResponseMessage() {
        return responsemessages.get(key);
    }

    public String ResponseBody() {
        return responsebodies.get(key);
    }

    public APIRequestContext Connection() {
        return requests.get(key);
    }

    public String HttpAgent() {
        return httpagents.get(key);
    }

    /**
     * ******************************
     */
}

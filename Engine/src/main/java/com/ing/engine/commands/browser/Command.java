package com.ing.engine.commands.browser;

import com.github.javafaker.Faker;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.image.ImageORObject;
import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.AutomationObject;
import com.ing.engine.drivers.MobileObject;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.drivers.SAPObject;
import com.ing.engine.drivers.StructuredDataObject;
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.drivers.AutomationObjectApi;
import com.ing.ingenious.api.contract.drivers.MobileObjectApi;
import com.ing.ingenious.api.contract.drivers.PlaywrightDriverCreationApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Kafka Imports */
// import org.apache.kafka.common.header.Header;
// import org.apache.avro.Schema;
// import org.apache.avro.generic.GenericRecord;
// import org.apache.kafka.clients.consumer.ConsumerRecord;
// import org.apache.kafka.clients.consumer.KafkaConsumer;
// import org.apache.kafka.clients.producer.KafkaProducer;
// import org.apache.kafka.clients.producer.ProducerRecord;

public class Command implements CommandPluginApi {
    public Page Page;
    public Playwright Playwright;
    public BrowserContext BrowserContext;
    public AutomationObject AObject;
    public MobileObject MObject;
    public StructuredDataObject SObject;
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

    //For SAP Testing
    public ActiveXComponent SAPsession;
    public SAPObject SAPObject;
    public Dispatch SAPElement;
    public Process SAPProcess;

    /**
     * ******API*******
     */
    public static Map<String, String> endPoints = new HashMap<>();
    public static Map<String, ArrayList<String>> headers = new HashMap<>();
    public static Map<String, ArrayList<String>> urlParams = new HashMap<>();
    public static Map<String, String> responsebodies = new HashMap<>();
    public static Map<String, String> responsecodes = new HashMap<>();
    public static Map<String, String> responsemessages = new HashMap<>();
    public static Map<String, APIRequestContext> requests = new HashMap<>();
    public static Map<String, APIResponse> responses = new HashMap<>();
    public static Map<String, java.net.http.HttpRequest.Builder> httpRequestBuilder = new HashMap<>();
    public static Map<String, java.net.http.HttpRequest> httpRequest = new HashMap<>();
    public static Map<String, java.net.http.HttpClient.Builder> httpClientBuilder = new HashMap<>();
    public static Map<String, java.net.http.HttpClient> httpClient = new HashMap<>();
    public static Map<String, java.net.http.HttpResponse> response = new HashMap<>();
    public static Map<String, String> httpagents = new HashMap<>();
    public static Map<String, Instant> before = new HashMap<>();
    public static Map<String, Instant> after = new HashMap<>();
    public static Map<String, Long> duration = new HashMap<>();
    public static HashMap<String, String> headerMap = new HashMap<>();
    public static Map<String, HashMap<String, String>> headerKeyValueMap = new HashMap<>();

    public String key;
    public static String basicAuthorization;
    /**
     * ************************
     */

    /**
     * Playwright Mocking *
     */
    public static Map<String, String> mockEndPoints = new HashMap<>();

    /**
     * ************************
     */

    /**
     * Data faker *
     */
    public static Map<String, Faker> faker = new HashMap<>();

    /**
     * ************************
     */

    /**
     * *** Queue ****
     */
    public static Map<String, String> jmsHost = new HashMap<>();
    public static Map<String, Integer> jmsPort = new HashMap<>();
    public static Map<String, String> jmsChannel = new HashMap<>();
    public static Map<String, String> jmsQmgr = new HashMap<>();
    public static Map<String, String> jmsUsername = new HashMap<>();
    public static Map<String, String> jmsPassword = new HashMap<>();
    public static Map<String, String> WMQ_SSL_CIPHER_SUITE = new HashMap<>();
    public static Map<String, String> jmsReqQueueName = new HashMap<>();
    public static Map<String, String> jmsRespQueueName = new HashMap<>();
    public static Map<String, JMSContext> jmsContext = new HashMap<>();
    public static Map<String, Destination> jmsDestination = new HashMap<>();
    public static Map<String, JMSProducer> jmsProducer = new HashMap<>();
    public static Map<String, JMSConsumer> jmsConsumer = new HashMap<>();
    public static Map<String, JmsFactoryFactory> jmsFactoryFactory = new HashMap<>();
    public static Map<String, JmsConnectionFactory> jmsConnectionFactory = new HashMap<>();
    public static Map<String, TextMessage> jmsMessage = new HashMap<>();
    public static Map<String, String> jmsCorrelationID = new HashMap<>();
    public static Map<String, String> receivedMessage = new HashMap<>();

    /**
     * **********
     */

    /**
     * *** Kafka Parameters ****
     */
    // static public Map<String, List<Header>> kafkaHeaders = new HashMap<>();
    // static public Map<String, String> kafkaProducerTopic = new HashMap<>();
    // static public Map<String, String> kafkaConsumerTopic = new HashMap<>();
    // static public Map<String, String> kafkaConsumerGroupId = new HashMap<>();
    // static public Map<String, String> kafkaServers = new HashMap<>();
    // static public Map<String, String> kafkaSchemaRegistryURL = new HashMap<>();
    // static public Map<String, Integer> kafkaPartition = new HashMap<>();
    // static public Map<String, Long> kafkaTimeStamp = new HashMap<>();
    // static public Map<String, String> kafkaKey = new HashMap<>();
    // static public Map<String, String> kafkaKeySerializer = new HashMap<>();
    // static public Map<String, String> kafkaKeyDeserializer = new HashMap<>();
    // static public Map<String, Object> kafkaValue = new HashMap<>();
    // static public Map<String, String> kafkaValueSerializer = new HashMap<>();
    // static public Map<String, String> kafkaValueDeserializer = new HashMap<>();
    // static public Map<String, Integer> kafkaConsumerPollRetries = new HashMap<>();
    // static public Map<String, Long> kafkaConsumerPollDuration = new HashMap<>();
    // static public Map<String, Schema> kafkaAvroSchema =new HashMap<>();
    // static public Map<String, ProducerRecord<String, GenericRecord>> kafkaGenericRecord =new HashMap<>();
    // static public Map<String, GenericRecord> kafkaGenericRecordValue =new HashMap<>();
    // static public Map<String, KafkaProducer<String, GenericRecord>> kafkaAvroProducer =new HashMap<>();
    // static public Map<String, ArrayList<String>> kafkaConfigs = new HashMap<>();
    // static public Map<String, Properties> kafkaProducersslConfigs = new HashMap<>();
    // static public Map<String, Properties> kafkaConsumersslConfigs = new HashMap<>();
    // static public Map<String, String> kafkaAvroCompatibleMessage = new HashMap<>();
    // static public Map<String, String> kafkaConsumeRecordCount = new HashMap<>();
    // static public Map<String, String> kafkaConsumeRecordValue = new HashMap<>();
    // static public Map<String, String> kafkaSharedSecret = new HashMap<>();
    // static public Map<String, List<ConsumerRecord<String, Object>>> kafkaConsumerRecords = new HashMap<>();
    // static public Map<String, ConsumerRecord<String, Object>> kafkaConsumerPollRecord = new HashMap<>();
    // static public Map<String, String> kafkaRecordIdentifierValue = new HashMap<>();
    // static public Map<String, String> kafkaRecordIdentifierPath = new HashMap<>();
    // static public Map<String, Integer> kafkaConsumerMaxPollRecords = new HashMap<>();
    // static public Map<String, Boolean> kafkaAutoRegisterSchemas = new HashMap<>();
    // static public Map<String, ProducerRecord> kafkaProducerRecord = new HashMap<>();
    // static public Map<String, ConsumerRecord> kafkaConsumerRecord = new HashMap<>();
    // static public Map<String, KafkaProducer> kafkaProducer = new HashMap<>();
    // static public Map<String, KafkaConsumer> kafkaConsumer = new HashMap<>();
    // static public Map<String, List<HashMap<String, String>>> kafkaRecordIdentifier = new HashMap<>();

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
        } else if (Commander.SAPsession != null) {
            SAPsession = Commander.SAPsession.session;
            SAPProcess = Commander.SAPsession.SAPProcess;
            SAPObject = Commander.SAPObject;
            Data = Commander.Data;
            ObjectName = Commander.ObjectName;
            SAPElement = Commander.SAPElement;
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
            SObject = Commander.SObject;
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
        key = userData.getScenario() + userData.getTestCase();
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

    public void addGlobalVar(String key, String val) {
        if (key.matches("%.*%")) {
            key = key.substring(1, key.length() - 1);
        }
        Commander.putUserDefinedData(key, val);
    }

    public String getUserDefinedData(String key) {
        return Commander.getUserDefinedData(key);
    }

    public String getDatasheet(String key) {
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

    public String resolveAllRuntimeVars(String str) {
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
     * Checks if a runtime or user-defined variable exists.
     *
     * <p>This method delegates to CommandControl's isVarExist method to verify whether
     * a variable is defined. See {@link CommandControl#isVarExist(String)} for details.</p>
     *
     * @param key the variable key to check, with or without percent signs (e.g., "%varName%" or "varName")
     * @return true if the variable exists and has a non-null value, false otherwise
     */
    public boolean isVarExist(String key) {
        return Commander.isVarExist(key);
    }

    /**
     * ******************************
     */

    /**
     * Implementation of {@link CommandPluginApi#getReport()} for the API-plugin contract.
     * @return the TestCaseReportApi instance for logging test results
     */
    @Override
    public TestCaseReportApi getReport() {
        return (TestCaseReportApi) Report;
    }

    /**
     * Implementation of {@link CommandPluginApi#getData()} for the API-plugin contract.
     * @return the data input parameter
     */
    @Override
    public String getData() {
        return Data;
    }

    /**
     * Implementation of {@link CommandPluginApi#getObjectName()} for the API-plugin contract.
     * @return the object name
     */
    @Override
    public String getObjectName() {
        return ObjectName;
    }

    /**
     * Implementation of {@link CommandPluginApi#getDescription()} for the API-plugin contract.
     * @return the action description
     */
    @Override
    public String getDescription() {
        return Description;
    }

    /**
     * Implementation of {@link CommandPluginApi#getCondition()} for the API-plugin contract.
     * @return the condition parameter
     */
    @Override
    public String getCondition() {
        return Condition;
    }

    /**
     * Implementation of {@link CommandPluginApi#getInput()} for the API-plugin contract.
     * @return the input parameter
     */
    @Override
    public String getInput() {
        return Input;
    }

    /**
     * Implementation of {@link CommandPluginApi#getAction()} for the API-plugin contract.
     * @return the action name
     */
    @Override
    public String getAction() {
        return Action;
    }

    /**
     * Implementation of {@link CommandPluginApi#getReference()} for the API-plugin contract.
     * @return the reference parameter
     */
    @Override
    public String getReference() {
        return Reference;
    }

    /**
     * Implementation of {@link CommandPluginApi#getUserData()} for the API-plugin contract.
     * @return the UserDataAccessApi instance for test data access
     */
    @Override
    public UserDataAccessApi getUserData() {
        return userData;
    }

    @Override
    public String getKey() {
        return key;
    }
}

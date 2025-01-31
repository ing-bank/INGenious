package com.ing.engine.commands.queue;

import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ing.engine.commands.browser.Command;
import static com.ing.engine.commands.browser.Command.after;
import static com.ing.engine.commands.browser.Command.before;
import static com.ing.engine.commands.browser.Command.duration;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.jayway.jsonpath.JsonPath;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class QueueOperations extends Command {

    public QueueOperations(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Host", input = InputType.YES, condition = InputType.NO)
    public void setHost() {
        try {
            jmsHost.put(key, Data);
            Report.updateTestLog(Action, "Queue Host has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Host: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Port", input = InputType.YES, condition = InputType.NO)
    public void setPort() {
        try {
            jmsPort.put(key, Integer.valueOf(Data));
            Report.updateTestLog(Action, "Queue Port has been set successfully", Status.DONE);
        } catch (NumberFormatException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Port: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Channel", input = InputType.YES, condition = InputType.NO)
    public void setChannel() {
        try {
            jmsChannel.put(key, Data);
            Report.updateTestLog(Action, "Queue Channel has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Channel: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Queue Manager", input = InputType.YES, condition = InputType.NO)
    public void setQueueManager() {
        try {
            jmsQmgr.put(key, Data);
            Report.updateTestLog(Action, "Queue manager has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Queue manager: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Username", input = InputType.YES, condition = InputType.NO)
    public void setUserName() {
        try {
            jmsUsername.put(key, Data);
            Report.updateTestLog(Action, "Username has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Username: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Password", input = InputType.YES, condition = InputType.NO)
    public void setPassword() {
        try {
            jmsPassword.put(key, Data);
            Report.updateTestLog(Action, "Password has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Password: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set SSL Cipher Suite", input = InputType.YES, condition = InputType.NO)
    public void setSSLCipherSuite() {
        try {
            WMQ_SSL_CIPHER_SUITE.put(key, Data);
            Report.updateTestLog(Action, "SSL Cipher Suite has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting SSL Cipher Suite: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Request Queue", input = InputType.YES, condition = InputType.NO)
    public void setRequestQueue() {
        try {
            if (Data.startsWith("queue:///")) {
                jmsReqQueueName.put(key, Data);
            } else {
                Data = "queue:///" + Data;
                jmsReqQueueName.put(key, Data);
            }
            Report.updateTestLog(Action, "Request Queue has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Request Queue: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Response Queue", input = InputType.YES, condition = InputType.NO)
    public void setResponseQueue() {
        try {
            if (Data.startsWith("queue:///")) {
                jmsRespQueueName.put(key, Data);
            } else {
                Data = "queue:///" + Data;
                jmsRespQueueName.put(key, Data);
            }
            Report.updateTestLog(Action, "Response Queue has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Response Queue: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    private void createConnectionFactory() {
        try {
            jmsFactoryFactory.put(key, JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER));
            jmsConnectionFactory.put(key, jmsFactoryFactory.get(key).createConnectionFactory());
            jmsConnectionFactory.get(key).setStringProperty(WMQConstants.WMQ_HOST_NAME, jmsHost.get(key));
            jmsConnectionFactory.get(key).setIntProperty(WMQConstants.WMQ_PORT, jmsPort.get(key));
            jmsConnectionFactory.get(key).setStringProperty(WMQConstants.WMQ_CHANNEL, jmsChannel.get(key));
            jmsConnectionFactory.get(key).setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, jmsQmgr.get(key));
            jmsConnectionFactory.get(key).setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            if (WMQ_SSL_CIPHER_SUITE.get(key) != null) {
                jmsConnectionFactory.get(key).setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, WMQ_SSL_CIPHER_SUITE.get(key));
            }
            if (jmsUsername.get(key) == null) {
                jmsConnectionFactory.get(key).setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, false);
                jmsContext.put(key, jmsConnectionFactory.get(key).createContext());
            } else {
                jmsContext.put(key, jmsConnectionFactory.get(key).createContext(jmsUsername.get(key), jmsPassword.get(key)));
            }
            jmsMessage.put(key, jmsContext.get(key).createTextMessage(""));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during JMS properties setup", ex);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Correlation ID", input = InputType.YES, condition = InputType.NO)
    public void setCorrelationID() {
        try {
            jmsMessage.get(key).setJMSCorrelationID(Data);
            Report.updateTestLog(Action, "Correlation ID set", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting of Correlation ID", ex);
            Report.updateTestLog(Action, "Error in setting Correlation ID: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Message ID", input = InputType.YES, condition = InputType.NO)
    public void setMesssageID() {
        try {
            jmsMessage.get(key).setJMSMessageID(Data);
            Report.updateTestLog(Action, "Message ID set", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting of Message ID", ex);
            Report.updateTestLog(Action, "Error in setting Message ID: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Text", input = InputType.YES, condition = InputType.NO)
    public void setText() {
        try {
            createConnectionFactory();
            jmsDestination.put(key, jmsContext.get(key).createQueue(jmsReqQueueName.get(key)));
            jmsMessage.get(key).setText(handlePayloadorEndpoint(Data));
            Report.updateTestLog(Action, "Text set", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting of Text", ex);
            Report.updateTestLog(Action, "Error in setting Text: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Send Message", input = InputType.NO, condition = InputType.NO)
    public void sendMessage() {
        try {
            jmsProducer.put(key, jmsContext.get(key).createProducer());
            before.put(key, Instant.now());
            jmsProducer.get(key).send(jmsDestination.get(key), jmsMessage.get(key));
            Report.updateTestLog(Action, "Message sent", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception while sending message", ex);
            Report.updateTestLog(Action, "Error in sending message: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Receive Message based on Filter", input = InputType.YES, condition = InputType.YES)
    public void receiveMessageWithFilter() {
        try {
            long timeout = Long.parseLong(Condition);
            jmsDestination.put(key, jmsContext.get(key).createQueue(jmsRespQueueName.get(key)));
            jmsConsumer.put(key, jmsContext.get(key).createConsumer(jmsDestination.get(key), Data));
            receivedMessage.put(key, jmsConsumer.get(key).receiveBody(String.class, timeout));
            after.put(key, Instant.now());
            duration.put(key, Duration.between(before.get(key), after.get(key)).toMillis());
            if (receivedMessage.get(key) != null) {
                Report.updateTestLog(Action, "Message received in : [" + duration.get(key) + "ms]. Message body is : \n" + receivedMessage, Status.DONE);
            } else {
                Report.updateTestLog(Action, "No Message received with filter " + Data, Status.DONE);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during receiving message", ex);
            Report.updateTestLog(Action, "Error in receiving message: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Close the connection", input = InputType.NO, condition = InputType.NO)
    public void closeContext() {
        try {
            jmsContext.get(key).close();
            Report.updateTestLog(Action, "Context closed", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during context closure", ex);
            Report.updateTestLog(Action, "Error while closing context: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    private String handlePayloadorEndpoint(String data) throws FileNotFoundException {
        String payloadstring = data;
        payloadstring = handleDataSheetVariables(payloadstring);
        payloadstring = handleuserDefinedVariables(payloadstring);
        System.out.println("Payload :" + payloadstring);
        return payloadstring;
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

    @Action(object = ObjectType.QUEUE, desc = "Store XML tag In DataSheet ", input = InputType.YES, condition = InputType.YES)
    public void storeQueueXMLtagInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String xmlText = receivedMessage.get(key);
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

    @Action(object = ObjectType.QUEUE, desc = "Assert XML Tag Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertQueueXMLtagEquals() {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(receivedMessage.get(key)));
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

    @Action(object = ObjectType.QUEUE, desc = "Assert XML Tag Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertQueueXMLtagContains() {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(receivedMessage.get(key)));
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

    @Action(object = ObjectType.QUEUE, desc = "Assert Response Message contains ", input = InputType.YES)
    public void assertQueueResponseMessageContains() {
        try {
            if (receivedMessage.get(key).contains(Data)) {
                Report.updateTestLog(Action, "Response Message contains : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Response Message does not contain : " + Data, Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response body :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Assert JSON Tag Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertQueueJSONtagEquals() {
        try {
            String response = receivedMessage.get(key);
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

    @Action(object = ObjectType.QUEUE, desc = "Assert JSON Tag Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertQueueJSONtagContains() {
        try {
            String response = receivedMessage.get(key);
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

    @Action(object = ObjectType.QUEUE, desc = "Store JSON Tag In DataSheet ", input = InputType.YES, condition = InputType.YES)
    public void storeQueueJSONtagInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String response = receivedMessage.get(key);
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

}

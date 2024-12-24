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
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            jmsReqQueueName.put(key, Data);
            Report.updateTestLog(Action, "Request Queue has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during queue connection setup", ex);
            Report.updateTestLog(Action, "Error in setting Request Queue: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Set Response Queue", input = InputType.YES, condition = InputType.NO)
    public void setResponseQueue() {
        try {
            jmsRespQueueName.put(key, Data);
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
            jmsConnectionFactory.get(key).setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, WMQ_SSL_CIPHER_SUITE.get(key));
            jmsConnectionFactory.get(key).setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, false);
            jmsContext.put(key, jmsConnectionFactory.get(key).createContext());

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

    @Action(object = ObjectType.QUEUE, desc = "Create and Send Message", input = InputType.YES, condition = InputType.NO)
    public void sendMessage() {
        try {
            jmsDestination.put(key, jmsContext.get(key).createQueue(jmsReqQueueName.get(key)));
            jmsMessage.put(key, jmsContext.get(key).createTextMessage(handlePayloadorEndpoint(Data)));
            jmsProducer.put(key, jmsContext.get(key).createProducer());
            before.put(key, Instant.now());
            jmsProducer.get(key).send(jmsDestination.get(key), jmsMessage.get(key));
            Report.updateTestLog(Action, "Message created and Sent", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Message Creation", ex);
            Report.updateTestLog(Action, "Error in sending message: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.QUEUE, desc = "Receive Message based on Filter", input = InputType.YES, condition = InputType.YES)
    public void receiveMessageWithFilter() {
        try {
            long timeout = Long.valueOf(Condition);
            jmsDestination.put(key, jmsContext.get(key).createQueue(jmsRespQueueName.get(key)));
            jmsConsumer.put(key, jmsContext.get(key).createConsumer(jmsDestination.get(key), Data));
            receivedMessage.put(key, jmsConsumer.get(key).receiveBody(String.class, timeout));
            after.put(key, Instant.now());
            duration.put(key, Duration.between(before.get(key), after.get(key)).toMillis());
            if (receivedMessage != null) {
                Report.updateTestLog(Action, "Message received in : [" + duration.get(key) + "ms]. Message body is : \n" + receivedMessage, Status.DONE);
            } else {
                Report.updateTestLog(Action, "No Message received with filter " + Data, Status.DONE);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during receiving message", ex);
            Report.updateTestLog(Action, "Error in receiving message: " + "\n" + ex.getMessage(), Status.DEBUG);
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

}

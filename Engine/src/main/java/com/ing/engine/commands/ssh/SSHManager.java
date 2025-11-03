package com.ing.engine.commands.ssh;

import com.google.common.net.UrlEscapers;
import com.ing.engine.commands.browser.General;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import com.jcraft.jsch.*;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSHManager extends General {

    public SSHManager(CommandControl cc) {
        super(cc);
    }

    /**
     * ****** Intermediary Host / Jumphost *******
     */

    @Action(object = ObjectType.SSH, desc = "Set SSH Intermediary Host", input = InputType.YES, condition = InputType.OPTIONAL)
    public void setSSHIntermediaryHost() throws JSchException {
        if (shouldExecute(Condition)) {
            try {
                System.out.println(Data);
                sshIntermediaryHost.put(Thread.currentThread().toString(), Data);
                Report.updateTestLog(Action, "SSH Intermediary Host has been set successfully", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during SSH Intermediary host connection setup", ex);
                Report.updateTestLog(Action, "Error in setting SSH Intermediary Host: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "No Intermediary host required, skipping step.", Status.DONE);
        }
    }

    @Action(object = ObjectType.SSH, desc = "Set SSH Intermediary Host Port", input = InputType.YES, condition = InputType.OPTIONAL)
    public void setSSHIntermediaryHostPort() {
        if (shouldExecute(Condition)) {
            try {
                sshIntermediaryHostPort.put(Thread.currentThread().toString(), Integer.valueOf(Data));
                Report.updateTestLog(Action, "SSH Port has been set successfully", Status.DONE);
            } catch (NumberFormatException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during SSH connection setup", ex);
                Report.updateTestLog(Action, "Error in setting SSH Port: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "No Intermediary host port required, skipping step.", Status.DONE);
        }
    }

    @Action(object = ObjectType.SSH, desc = "Set SSH Intermediary Host Username", input = InputType.YES, condition = InputType.NO)
    public void setSSHIntermediaryHostUserName() {
        if (shouldExecute(Condition)) {
            try {
                sshIntermediaryHostUsername.put(Thread.currentThread().toString(), Data);
                Report.updateTestLog(Action, "Intermediary Host Username has been set successfully", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Intermediary Host Username SSH connection setup", ex);
                Report.updateTestLog(Action, "Error in setting Intermediary Host Username: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "No Intermediary host required, skipping step.", Status.DONE);
        }
    }

    @Action(object = ObjectType.SSH, desc = "Set SSH Intermediary Host Password", input = InputType.YES, condition = InputType.OPTIONAL)
    public void setSSHIntermediaryHostPassword() {
        if (shouldExecute(Condition)) {
            try {
                sshIntermediaryHostPassword.put(Thread.currentThread().toString(), Data);
                Report.updateTestLog(Action, "SSH Intermediary Host Password has been set successfully", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting SSH Intermediary Host Password", ex);
                Report.updateTestLog(Action, "Error in setting Password: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "No Intermediary host required, skipping step.", Status.DONE);
        }
    }

    @Action(object = ObjectType.SSH, desc = "Set SSH Host Port for Local Port Forwarding", input = InputType.YES, condition = InputType.NO)
    public void setSSHIntermediateHostTunnelPort() {
        if (shouldExecute(Condition)) {
            try {
                sshIntermediaryHostTunnelPort.put(Thread.currentThread().toString(), Integer.valueOf(Data));
                Report.updateTestLog(Action, "SSH Intermediate Host Tunnel Port has been set successfully", Status.DONE);
            } catch (NumberFormatException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during SSH Intermediate Host Tunnel Port setup", ex);
                Report.updateTestLog(Action, "Error in setting SSH Intermediate Host Tunnel Port: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "No Intermediary host tunnel port required, skipping step.", Status.DONE);
        }
    }

    /**
     * ************************
     */

    /**
     * ****** Target Host *******
     */

    @Action(object = ObjectType.SSH, desc = "Set SSH Host", input = InputType.YES)
    public void setSSHHost() throws JSchException {
        if (shouldExecute(Condition)) {
            try {
                System.out.println(Data);
                sshHost.put(Thread.currentThread().toString(), Data);
                Report.updateTestLog(Action, "SSH Host has been set successfully", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during SSH connection setup", ex);
                Report.updateTestLog(Action, "Error in setting SSH Host: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Set SSH Host Port", input = InputType.YES, condition = InputType.NO)
    public void setSSHHostPort() {
        if (shouldExecute(Condition)) {
            try {
                sshHostPort.put(Thread.currentThread().toString(), Integer.valueOf(Data));
                Report.updateTestLog(Action, "SSH Port has been set successfully", Status.DONE);
            } catch (NumberFormatException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during SSH connection setup", ex);
                Report.updateTestLog(Action, "Error in setting SSH Port: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Set SSH Host Username", input = InputType.YES, condition = InputType.NO)
    public void setSSHHostUserName() {
        if (shouldExecute(Condition)) {
            try {
                sshHostUsername.put(Thread.currentThread().toString(), Data);
                Report.updateTestLog(Action, "Username has been set successfully", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during SSH connection setup", ex);
                Report.updateTestLog(Action, "Error in setting Username: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Set SSH Host Password", input = InputType.YES, condition = InputType.NO)
    public void setSSHHostPassword() {
        if (shouldExecute(Condition)) {
            try {
                sshHostPassword.put(Thread.currentThread().toString(), Data);
                Report.updateTestLog(Action, "Password has been set successfully", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during SSH connection setup", ex);
                Report.updateTestLog(Action, "Error in setting Password: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

    /**
     * ************************
     */


    @Action(object = ObjectType.SSH, desc = "Create SSH Session", input = InputType.NO, condition = InputType.NO)
    public void createSSHSession() {
        if (!shouldExecute(Condition)) return;

        Exception caughtException = null;
        for (int i = 0; i < 3; i++) {
            try {
                if (sshIntermediaryHost.get(Thread.currentThread().toString()) != null) {
                    createSessionViaIntermediary();
                } else {
                    createDirectSession();
                }

                return;
            } catch (Exception ex) {
                caughtException = ex;
            }
        }

        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Exception during SSH connection setup", caughtException);
        Report.updateTestLog(Action, "Session creation failed: " + "\n" + caughtException.getMessage(), Status.FAIL);
    }

    private void createDirectSession() throws JSchException {
        if (sshHostSession.get(Thread.currentThread().toString()) == null || !sshHostSession.get(Thread.currentThread().toString()).isConnected()) {
            JSch jsch = new JSch();
            Session sessionDirect = jsch.getSession(sshHostUsername.get(Thread.currentThread().toString()), sshHost.get(Thread.currentThread().toString()), sshHostPort.get(Thread.currentThread().toString()));
            sessionDirect.setPassword(sshHostPassword.get(Thread.currentThread().toString()));
            sessionDirect.setConfig("StrictHostKeyChecking", "no");
            sessionDirect.setTimeout(15000); // Set timeout for connection
            sessionDirect.connect();
            sshHostSession.put(Thread.currentThread().toString(), sessionDirect);
            Report.updateTestLog(Action, "SSH session created successfully", Status.DONE);
        } else {
            Report.updateTestLog(Action, "SSH session reused successfully", Status.DONE);
        }
    }

    private void createSessionViaIntermediary() throws JSchException, InterruptedException {
        if (sshIntermediaryHostSession.get(Thread.currentThread().toString()) == null || !sshIntermediaryHostSession.get(Thread.currentThread().toString()).isConnected() || sshHostSession.get(Thread.currentThread().toString()) == null || !sshHostSession.get(Thread.currentThread().toString()).isConnected()) {
            JSch jsch = new JSch();

            // Enable if debugging is required
//             JSch.setLogger(new SSHLogger());
            waitForMFASession();

            Session sessionIntermediary = createIntermediarySession(jsch);
            sshIntermediaryHostSession.put(Thread.currentThread().toString(), sessionIntermediary);

            int availablePort = setupPortForwarding(sessionIntermediary);

            Session sessionViaTunnel = createTunnelSession(jsch, availablePort);
            sshHostSession.put(Thread.currentThread().toString(), sessionViaTunnel);
            Report.updateTestLog(Action, "SSH session created successfully via intermediary host", Status.DONE);
        } else {
            Report.updateTestLog(Action, "SSH Intermediary Host Session session reused successfully", Status.DONE);
        }
    }

    private void waitForMFASession() throws InterruptedException {
        int MIN_VALUE = 5000;
        int MAX_VALUE = 10000;
        Random random = new Random();
        int myRandomNumber = random.nextInt(MAX_VALUE - MIN_VALUE) + MIN_VALUE;
        System.out.println("Waiting on MFA. Wait time: " + myRandomNumber);
        Thread.sleep(myRandomNumber);
    }

    private Session createIntermediarySession(JSch jsch) throws JSchException {
        Session sessionIntermediary = jsch.getSession(sshIntermediaryHostUsername.get(Thread.currentThread().toString()), sshIntermediaryHost.get(Thread.currentThread().toString()), sshIntermediaryHostPort.get(Thread.currentThread().toString()));
        System.out.println(sshIntermediaryHostUsername.get(Thread.currentThread().toString()));
        System.out.println(sshIntermediaryHost.get(Thread.currentThread().toString()));
        System.out.println(sshIntermediaryHostPort.get(Thread.currentThread().toString()));
        System.out.println(sshIntermediaryHostPassword.get(Thread.currentThread().toString()));
        sessionIntermediary.setPassword(sshIntermediaryHostPassword.get(Thread.currentThread().toString()));
        sessionIntermediary.setConfig("StrictHostKeyChecking", "no");

        // Enable if debugging is required.
        // for (Provider x : Security.getProviders()) {
        //  System.out.println(x.getName() + " " + x.getInfo() + " " + x.getVersionStr());
        // }

        sessionIntermediary.connect();
        return sessionIntermediary;
    }

    private int setupPortForwarding(Session sessionIntermediary) throws JSchException {
        int availablePort;
        if (sshIntermediaryHostTunnelPort.get(Thread.currentThread().toString()) == null) {
            availablePort = getAvailablePort();
            System.out.println("Available port taken: " + availablePort);
        } else {
            availablePort = sshIntermediaryHostTunnelPort.get(Thread.currentThread().toString());
            System.out.println("Provided port taken: " + availablePort);
        }
        sessionIntermediary.setPortForwardingL(availablePort, sshHost.get(Thread.currentThread().toString()), sshHostPort.get(Thread.currentThread().toString()));
        return availablePort;
    }

    private Session createTunnelSession(JSch jsch, int availablePort) throws JSchException {
        Session sessionViaTunnel = jsch.getSession(sshHostUsername.get(Thread.currentThread().toString()), "localhost", availablePort);
        sessionViaTunnel.setPassword(sshHostPassword.get(Thread.currentThread().toString()));
        sessionViaTunnel.setConfig("StrictHostKeyChecking", "no");
        sessionViaTunnel.connect();
        return sessionViaTunnel;
    }


    @Action(object = ObjectType.SSH, desc = "Run Command via SSH Channel", input = InputType.YES, condition = InputType.NO)
    public void runCommand() {
        if (shouldExecute(Condition)) {
            try {
                // Check to reuse or create new sessions.
                disconnectChannels();
                createSSHSession();

                if (sshHostChannelExec.get(Thread.currentThread().toString()) == null) {
                    ChannelExec channelExec = (ChannelExec) sshHostSession.get(Thread.currentThread().toString()).openChannel("exec");
                    sshHostChannelExec.put(Thread.currentThread().toString(), channelExec);
                    sshHostChannelExec.get(Thread.currentThread().toString()).setCommand(Data);

                    // Capture command output (stdout)
                    InputStream inputStream = sshHostChannelExec.get(Thread.currentThread().toString()).getInputStream();
                    InputStream errorStream = sshHostChannelExec.get(Thread.currentThread().toString()).getErrStream();

                    sshHostChannelExec.get(Thread.currentThread().toString()).connect(60000);
                    System.out.println("Executing command: " + Data);

                    StringBuilder output = new StringBuilder();

                    // Read Standard Output
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    System.out.println("Command output:");
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
//                        System.out.println(line);
                    }

                    // Read error output (if any)
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    while ((line = errorReader.readLine()) != null) {
                        output.append(line).append("\n");
//                        System.err.println("ERROR: " + line);
                    }

                    // 2021-03-24 16:48:05
                    SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd---HH-mm-ss-S");
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                    String logPrefix = userData.getCurrentScenario() + "_" + userData.getCurrentTestCase();
                    File commandOutput = new File(FilePath.getCurrentTestCaseLogsLocation() + File.separator + sdf3.format(timestamp) + ".txt");

//                    System.out.println(output.toString());
                    sshCommandOutput.put(Thread.currentThread().toString(), output.toString());
                    FileUtils.writeStringToFile(commandOutput, output.toString(), (Charset) null);

                    // Wait for command completion
                    int exitStatus = sshHostChannelExec.get(Thread.currentThread().toString()).getExitStatus();
                    System.out.println("Exit Status: " + exitStatus);
                    String logPrefixEncoded = UrlEscapers.urlFragmentEscaper().escape(logPrefix);
                    Report.updateTestLog(Action, "Command: " + Data + "\nwas executed with result logged at" + "\n<a href=logs\\" + sdf3.format(timestamp) + ".txt" + ">" + commandOutput.getName() + "</a>", Status.PASS);

                } else if (sshHostChannelExec.get(Thread.currentThread().toString()).isConnected()) {
                    // Capture command output (stdout)
                    InputStream inputStream = sshHostChannelExec.get(Thread.currentThread().toString()).getInputStream();
                    InputStream errorStream = sshHostChannelExec.get(Thread.currentThread().toString()).getErrStream();

                    System.out.println("Executing command: " + Data);

                    StringBuilder output = new StringBuilder();

                    // Read Standard Output
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    System.out.println("Command output:");
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
//                        System.out.println(line);
                    }

                    // Read error output (if any)
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    while ((line = errorReader.readLine()) != null) {
                        output.append(line).append("\n");
//                        System.err.println("ERROR: " + line);
                    }

                    // 2021-03-24 16:48:05
                    SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd---HH-mm-ss-S");
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                    String logPrefix = userData.getCurrentScenario() + "_" + userData.getCurrentTestCase();
                    File commandOutput = new File(FilePath.getCurrentTestCaseLogsLocation() + File.separator + sdf3.format(timestamp) + ".txt");

//                    System.out.println(output.toString());
                    sshCommandOutput.put(Thread.currentThread().toString(), output.toString());
                    FileUtils.writeStringToFile(commandOutput, output.toString(), (Charset) null);

                    // Wait for command completion
                    int exitStatus = sshHostChannelExec.get(Thread.currentThread().toString()).getExitStatus();
                    System.out.println("Exit Status: " + exitStatus);
                    Report.updateTestLog(Action, "Command: " + Data + "\nwas executed with result logged at" + "\n<a href=logs\\" + sdf3.format(timestamp) + ".txt" + ">" + commandOutput.getName() + "</a>", Status.PASS);

                } else {
                    ChannelExec channelExec = (ChannelExec) sshHostSession.get(Thread.currentThread().toString()).openChannel("exec");
                    sshHostChannelExec.put(Thread.currentThread().toString(), channelExec);
                    sshHostChannelExec.get(Thread.currentThread().toString()).setCommand(Data);

                    // Capture command output (stdout)
                    InputStream inputStream = sshHostChannelExec.get(Thread.currentThread().toString()).getInputStream();
                    InputStream errorStream = sshHostChannelExec.get(Thread.currentThread().toString()).getErrStream();

                    sshHostChannelExec.get(Thread.currentThread().toString()).connect(60000);
                    System.out.println("Executing command: " + Data);

                    StringBuilder output = new StringBuilder();

                    // Read Standard Output
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    System.out.println("Command output:");
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
//                        System.out.println(line);
                    }

                    // Read error output (if any)
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    while ((line = errorReader.readLine()) != null) {
                        output.append(line).append("\n");
//                        System.err.println("ERROR: " + line);
                    }

                    // 2021-03-24 16:48:05
                    SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd---HH-mm-ss-S");
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                    String logPrefix = userData.getCurrentScenario() + "_" + userData.getCurrentTestCase();
                    File commandOutput = new File(FilePath.getCurrentTestCaseLogsLocation() + File.separator + sdf3.format(timestamp) + ".txt");

//                    System.out.println(output.toString());
                    sshCommandOutput.put(Thread.currentThread().toString(), output.toString());
                    FileUtils.writeStringToFile(commandOutput, output.toString(), (Charset) null);

                    // Wait for command completion
                    int exitStatus = sshHostChannelExec.get(Thread.currentThread().toString()).getExitStatus();
                    System.out.println("Exit Status: " + exitStatus);
                    Report.updateTestLog(Action, "Command: " + Data + "\nwas executed with result logged at" + "\n<a href=logs\\" + sdf3.format(timestamp) + ".txt" + ">" + commandOutput.getName() + "</a>", Status.PASS);
                }

            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception command execution", ex);
                Report.updateTestLog(Action, "Error in command execution: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Assert Command Output", input = InputType.YES, condition = InputType.NO)
    public void assertCommandOutput() {
        if (shouldExecute(Condition)) {
            try {
                Assert.assertTrue(sshCommandOutput.get(Thread.currentThread().toString()).contains(Data));
                Report.updateTestLog(Action, "Substring assertion executed successfully", Status.DONE);
            } catch (AssertionFailedError ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Exception during assertion substring SSH command output", ex);
                Report.updateTestLog(Action, "Error in assertion substring SSH command output: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Retrieve Value from Command Output", input = InputType.YES, condition = InputType.NO)
    public void retrieveValueFromCommandOutput() {
        if (shouldExecute(Condition)) {
            try {
                // Compile the pattern
                Pattern pattern = Pattern.compile(Data);

                // Create a matcher for the console output
                Matcher matcher = pattern.matcher(sshCommandOutput.get(Thread.currentThread().toString()));

                // Find and retrieve the value
                if (matcher.find()) {
                    String value = matcher.group(1);
                    System.out.println("Retrieved value: " + value);
                    regExCommandOutputResult.put(Thread.currentThread().toString(), value);
                    addVar(Condition, value);


                    Report.updateTestLog(Action, "Retrieving value using regex executed successfully", Status.DONE);
                } else {
                    System.out.println("Value not found.");
                    Report.updateTestLog(Action, "Retrieving value using regex returned no result", Status.FAIL);
                }

            } catch (AssertionFailedError ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Exception during assertion substring SSH command output", ex);
                Report.updateTestLog(Action, "Error in assertion substring SSH command output: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

//    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Branch Code", input = InputType.NO, condition = InputType.YES)
//    public void getCommandOutputResult() throws JSchException {
//        try {
//            addVar(Condition, regExCommandOutputResult.get(Thread.currentThread().toString()));
//            Report.updateTestLog(Action, "Command Output Result " + Data + " saved successfully in " + Condition, Status.DONE);
//        } catch (Exception ex) {
//            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban branch code", ex);
//            Report.updateTestLog(Action, "Error in saving iban branch code: " + "\n" + ex.getMessage(), Status.DEBUG);
//        }
//    }

    @Action(object = ObjectType.SSH, desc = "Set Destination Folder", input = InputType.YES, condition = InputType.NO)
    public void setDestinationFolder() {
        if (shouldExecute(Condition)) {
            try {
                sshDestinationFolder.put(Thread.currentThread().toString(), Data);
                Report.updateTestLog(Action, "Destination Folder set successfully", Status.DONE);
            } catch (NumberFormatException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting Destination Folder", ex);
                Report.updateTestLog(Action, "Error in setting Destination Folder: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Copy File via SSH Channel", input = InputType.NO, condition = InputType.NO)
    public void copyFile() {
        if (shouldExecute(Condition)) {
            try {
                // Check to reuse or create new sessions.
                disconnectChannels();
                createSSHSession();

                if (sshHostChannelSftp.get(Thread.currentThread().toString()) == null) {
                    ChannelSftp channelSftp = (ChannelSftp) sshHostSession.get(Thread.currentThread().toString()).openChannel("sftp");
                    sshHostChannelSftp.put(Thread.currentThread().toString(), channelSftp);

                    sshHostChannelSftp.get(Thread.currentThread().toString()).connect(60000);
                    sshHostChannelSftp.get(Thread.currentThread().toString()).cd(sshDestinationFolder.get(Thread.currentThread().toString()));

                    String fileName = getVar("%fileName%");
                    String fileLocation = getVar("%fileLocation%");

                    sshHostChannelSftp.get(Thread.currentThread().toString()).put(new FileInputStream(fileLocation + File.separator + fileName), fileName);

                    // Wait for command completion
                    int exitStatus = sshHostChannelSftp.get(Thread.currentThread().toString()).getExitStatus();
                    System.out.println("Exit Status: " + exitStatus);
                    Report.updateTestLog(Action, "File Copied successfully", Status.DONE);
                } else if (sshHostChannelSftp.get(Thread.currentThread().toString()).isConnected()) {
                    sshHostChannelSftp.get(Thread.currentThread().toString()).cd(sshDestinationFolder.get(Thread.currentThread().toString()));

                    String fileName = getVar("%fileName%");
                    String fileLocation = getVar("%fileLocation%");

                    sshHostChannelSftp.get(Thread.currentThread().toString()).put(new FileInputStream(fileLocation + File.separator + fileName), fileName);

                    // Wait for command completion
                    int exitStatus = sshHostChannelSftp.get(Thread.currentThread().toString()).getExitStatus();
                    System.out.println("Exit Status: " + exitStatus);
                    Report.updateTestLog(Action, "File " + fileName + " Copied successfully", Status.DONE);
                } else {
                    ChannelSftp channelSftp = (ChannelSftp) sshHostSession.get(Thread.currentThread().toString()).openChannel("sftp");
                    sshHostChannelSftp.put(Thread.currentThread().toString(), channelSftp);

                    sshHostChannelSftp.get(Thread.currentThread().toString()).connect(60000);
                    sshHostChannelSftp.get(Thread.currentThread().toString()).cd(sshDestinationFolder.get(Thread.currentThread().toString()));

                    String fileName = getVar("%fileName%");
                    String fileLocation = getVar("%fileLocation%");

                    sshHostChannelSftp.get(Thread.currentThread().toString()).put(new FileInputStream(fileLocation + File.separator + fileName), fileName);

                    // Wait for command completion
                    int exitStatus = sshHostChannelSftp.get(Thread.currentThread().toString()).getExitStatus();
                    System.out.println("Exit Status: " + exitStatus);
                    Report.updateTestLog(Action, "File " + fileName + " Copied successfully", Status.DONE);
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception command execution", ex);
                Report.updateTestLog(Action, "Error in copy file execution: " + "\n" + ex.getMessage(), Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Disconnect SSH Channels", input = InputType.NO, condition = InputType.NO)
    public void disconnectChannels() {
        if (shouldExecute(Condition)) {
            try {
                if (sshHostChannelExec.get(Thread.currentThread().toString()) != null) {
                    System.out.println("Closing Exec channel...");
                    sshHostChannelExec.get(Thread.currentThread().toString()).disconnect();
                    sshHostChannelExec.remove(Thread.currentThread().toString());
                    Report.updateTestLog(Action, "Exec Channel Closed", Status.DONE);
                }
                if (sshHostChannelSftp.get(Thread.currentThread().toString()) != null) {
                    System.out.println("Closing Sftp channel...");
                    sshHostChannelSftp.get(Thread.currentThread().toString()).disconnect();
                    sshHostChannelSftp.remove(Thread.currentThread().toString());
                    Report.updateTestLog(Action, "Sftp Channel Closed", Status.DONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Report.updateTestLog(Action, "Error disconnecting channels", Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Disconnect Session", input = InputType.NO, condition = InputType.NO)
    public void disconnectSession() {
        if (shouldExecute(Condition)) {
            try {
                if (sshHostSession.get(Thread.currentThread().toString()) != null && sshHostSession.get(Thread.currentThread().toString()).isConnected()) {
                    System.out.println("Disconnecting session(s)...");
                    sshHostSession.get(Thread.currentThread().toString()).disconnect();
                    sshHostSession.remove(Thread.currentThread().toString());
                    System.out.println("Closed SSH Host Session...");
                    Report.updateTestLog(Action, "SSH Host Session(s) Closed", Status.DONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Report.updateTestLog(Action, "Error disconnecting session", Status.FAIL);
            }
        }
    }

    @Action(object = ObjectType.SSH, desc = "Disconnect Intermediary Host Session", input = InputType.NO, condition = InputType.NO)
    public void disconnectIntermediarySession() {
        if (shouldExecute(Condition)) {
            try {
                if (sshIntermediaryHostSession.get(Thread.currentThread().toString()) != null && sshIntermediaryHostSession.get(Thread.currentThread().toString()).isConnected()) {
                    System.out.println("Disconnecting Intermediary Host session(s)...");
                    sshIntermediaryHostSession.get(Thread.currentThread().toString()).disconnect();
                    sshIntermediaryHostSession.remove(Thread.currentThread().toString());
                    System.out.println("Closed Intermediary SSH Host Session...");
                    Report.updateTestLog(Action, "Intermediary Session(s) Closed", Status.DONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Report.updateTestLog(Action, "Error disconnecting intermediary session", Status.FAIL);
            }
        }
    }

    public static int getAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0) ) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("No available ports found", e);
        }
    }

}
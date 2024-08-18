
package com.ing.ide.main.utils;

import com.ing.ide.main.ui.About;
import com.ing.ide.util.Notification;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import org.apache.commons.io.FileUtils;

public class CMProjectCreator {

    private static CMProjectCreator projCreator;

    private final File engineLoc = new File("Engine");

    private final File dotproject = new File(engineLoc, ".project");

    private final File dotClassPath = new File(engineLoc, ".classpath");

    private final File sampleScript = new File("Configuration" + File.separator + "SampleScript.java");

    private final String enginePath
            = "<classpathentry kind=\"lib\" path=\"../lib/ingenious-engine-"
            + About.getBuildVersion()
            + ".jar\"/>\n";

    public static void createCMProject() {
        if (projCreator == null) {
            projCreator = new CMProjectCreator();
        }
        projCreator.create();
    }

    private void create() {
        File location = openDirectory();
        if (location != null) {
            File newProject = new File(location, "Custom_Method_Project");
            newProject.mkdirs();
            createDotProject(newProject);
            copyClassPath(newProject);
            createSampleCode(newProject);
            Notification.show("[Custom_Method] Project Created");
        }
    }

    private void createDotProject(File location) {
        try {
            String content = FileUtils.readFileToString(dotproject, Charset.defaultCharset());
            content = content.replaceFirst("<name>(.*)<\\/name>", "<name>Custom_Method_Project<\\/name>");
            File projFile = new File(location, dotproject.getName());
            FileUtils.writeStringToFile(projFile, content, Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.getLogger(CMProjectCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void copyClassPath(File location) {
        try {
            String content = FileUtils.readFileToString(dotClassPath, Charset.defaultCharset());
            content = content.replace("</classpath>", enginePath + "</classpath>");
            content = content.replace("..", new File("").getAbsolutePath());
            File classFile = new File(location, dotClassPath.getName());
            FileUtils.writeStringToFile(classFile, content, Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.getLogger(CMProjectCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createSampleCode(File location) {
        try {
            File src = new File(location, "src");
            new File(src, "test" + File.separator + "java").mkdirs();
            new File(src, "main" + File.separator + "resources").mkdirs();
            File packageF = new File(src, "main" + File.separator + "java" + File.separator + "sample");
            packageF.mkdirs();
            String content = FileUtils.readFileToString(sampleScript, Charset.defaultCharset());
            content = "package sample; \n".concat(content);
            File codeFile = new File(packageF, sampleScript.getName());
            FileUtils.writeStringToFile(codeFile, content, Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.getLogger(CMProjectCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private File openDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setApproveButtonText("Create Project");
        fileChooser.setDialogTitle("Select the Directory to Create the Eclipse Project");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = fileChooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

}

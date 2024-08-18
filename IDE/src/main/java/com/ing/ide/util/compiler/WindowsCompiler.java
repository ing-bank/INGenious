
package com.ing.ide.util.compiler;

import com.ing.engine.support.DesktopApi;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class WindowsCompiler extends Compiler {

    public WindowsCompiler(String src, String lib, String jpath) {
        super(src, lib, jpath);
    }

    @Override
    public File createScript() {

        String script = getScriptFile();

        try (BufferedWriter out = new BufferedWriter(new FileWriter(script))) {
            out.write("@echo off");
            out.newLine();
            out.write("pushd %~dp0");
            out.newLine();
            out.write("PATH " + javaPath);
            out.newLine();
            out.write(getCompileScript());
            out.newLine();
            out.write("set /p key=Completed. Hit ENTER to Exit");
            out.newLine();
        } catch (Exception ex) {
            Logger.getLogger(WindowsCompiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new File(script);
    }

    @Override
    public String getScriptFile() {
        
        String userDirlocation;
        try {
            userDirlocation = new File(System.getProperty("user.dir")).getCanonicalPath();
            return userDirlocation + File.separator + "CompileRunScript.bat";
        } catch (IOException ex) {
            Logger.getLogger(WindowsCompiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
        
    }

    @Override
    String getSep() {
        return ";";
    }

    @Override
    public String compile() {
        if (DesktopApi.open(new File(getScriptFile()))) {
            return "true";
        }
        return "false";
    }

}

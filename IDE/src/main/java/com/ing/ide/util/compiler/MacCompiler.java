
package com.ing.ide.util.compiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 *
 *
 */
public class MacCompiler extends Compiler {

    public MacCompiler(String src, String lib, String jpath) {
        super(src, lib, jpath);
    }

    @Override
    public File createScript() {

        String script = getScriptFile();

        try (BufferedWriter out = new BufferedWriter(new FileWriter(script))) {
            out.newLine();
            out.write("cd \"" + System.getProperty("user.dir") + "\"");
            out.newLine();
            out.write(setPathScript());
            out.newLine();
            out.write(getCompileScript());
            out.newLine();
            out.newLine();
        } catch (Exception ex) {
            Logger.getLogger(MacCompiler.class.getName()).log(Level.SEVERE, null, ex);

        }
        return new File(script);
    }

    @Override
    String getSep() {
        return ":";
    }

    @Override
    public String getScriptFile() {
        try {
            String userDirlocation = new File(System.getProperty("user.dir")).getCanonicalPath();
            return userDirlocation + File.separator + "CompileRunScript.sh";
        } catch (IOException ex) {
            Logger.getLogger(MacCompiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public String compile() {
        try {

            final ProcessBuilder pb = new ProcessBuilder("/bin/sh", getScriptFile());
            pb.directory(new File(new File(System.getProperty("user.dir")).getCanonicalPath()));
            final Process p = pb.start();
            p.waitFor();
            String sb = IOUtils.toString(p.getErrorStream(), "UTF-8");
            if (!"".equals(sb)) {
                return sb;
            }
            return "Completed.";
        } catch (Exception ex) {
            Logger.getLogger(MacCompiler.class.getName()).log(Level.SEVERE, null, ex);
            return ex.getMessage();
        }

    }

    @Override
    public String setPathScript() {
        return "PATH=\"" + javaPath + "\":$PATH;";
    }
}

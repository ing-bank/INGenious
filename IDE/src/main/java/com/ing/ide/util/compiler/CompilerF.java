
package com.ing.ide.util.compiler;

import java.io.File;

/**
 *
 * 
 */
public interface CompilerF {

    /**
     * create a script file to compile the java file
     *
     * @return compile script
     */
    public String getScriptFile();

    /**
     * create a script file to compile the java file
     *
     * @return compile script
     */
    public File createScript();

    /**
     * compile the java file
     *
     * @return the status of the process
     */
    public String compile();
}

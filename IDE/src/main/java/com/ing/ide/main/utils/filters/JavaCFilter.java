
package com.ing.ide.main.utils.filters;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * 
 */
public class JavaCFilter extends FileFilter{
    @Override
    public boolean accept(File file){
       return file.isDirectory() && 
               (new File(file,"javac.exe").exists() || new File(file,"javac").exists());
    }
    @Override
    public String getDescription() {
        return "JDK Bin Directory";
    }
}

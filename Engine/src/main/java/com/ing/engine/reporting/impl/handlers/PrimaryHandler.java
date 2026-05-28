
package com.ing.engine.reporting.impl.handlers;

import com.ing.ingenious.api.status.Status;
import java.io.File;

/**
 *
 * 
 */
public interface PrimaryHandler {
    
    
 public Object getData();
 
 public File getFile();

    public Status getCurrentStatus();
 
 
    
}

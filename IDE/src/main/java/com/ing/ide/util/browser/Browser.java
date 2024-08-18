
package com.ing.ide.util.browser;

import java.awt.Component;

/**
 *
 * 
 */
public interface Browser {

    public void load();

    public Component getComponent();

    public void setUrl(String url);

    public void quit();

    public void back();

    public void next();
    
    public String getUrl();

}

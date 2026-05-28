package com.ing.datalib.settings;

public class LambdaTestCaps extends AbstractPropSettings {

    public LambdaTestCaps(String location) {
        super(location, "lambdatestcapabilities");
        if (isEmpty()) {
            loadDefault();
        }
    }

    private void loadDefault() {
        put("build", "");
        put("user", "");
        put("accessKey", "");
        put("video", "true");
        put("console", "true");
        put("network", "true");
        put("resolution", "1920x1080");
        put("visual","true");
        put("tunnel","false");
        put("tunnelName","");
        put("geoLocation","");
        put("idleTimeout","300");
       // put("lambdaMaskCommands","");
        put("useSpecificBundleVersion","false");
        
    }
    
}

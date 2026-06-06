package com.ing.datalib.settings.emulators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a mobile device entry managed under the "Manage Devices" tab.
 * Kept intentionally minimal: a name plus a flag indicating whether the
 * device should be treated as a LambdaTest device (which determines which
 * default capability set is presented in the IDE).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"Name", "LambdaTest", "Remote URL"})
public class Device {

    public static final String DEFAULT_REMOTE_URL = "http://127.0.0.1:4723/";

    @JsonProperty("Name")
    private String name;

    @JsonProperty("LambdaTest")
    private boolean lambdaTest;

    @JsonProperty("Remote URL")
    private String remoteUrl;

    public Device() {
    }

    public Device(String name) {
        this.name = name;
        this.lambdaTest = false;
        this.remoteUrl = DEFAULT_REMOTE_URL;
    }

    @JsonProperty("Name")
    public String getName() {
        return name;
    }

    @JsonProperty("Name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("LambdaTest")
    public boolean isLambdaTest() {
        return lambdaTest;
    }

    @JsonProperty("LambdaTest")
    public void setLambdaTest(boolean lambdaTest) {
        this.lambdaTest = lambdaTest;
    }

    @JsonProperty("Remote URL")
    public String getRemoteUrl() {
        return remoteUrl;
    }

    @JsonProperty("Remote URL")
    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }
}


package com.ing.datalib.settings.emulators;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "Name",
    "Type",
    "AppiumURL",
    "Size",
    "Driver",
    "UserAgent"
})
public class Emulator {

    @JsonProperty("Name")
    private String name;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Remote URL")
    private String remoteUrl;
    @JsonProperty("Size")
    private String size;
    @JsonProperty("Driver")
    private String driver;
    @JsonProperty("UserAgent")
    private String userAgent;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    public Emulator() {
    }

    public Emulator(String name) {
        this.name = name;
        this.type = "Emulator";
        this.driver = "Chrome";
    }

    /**
     *
     * @return The name
     */
    @JsonProperty("Name")
    public String getName() {
        return name;
    }

    /**
     *
     * @param name The Name
     */
    @JsonProperty("Name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return The type
     */
    @JsonProperty("Type")
    public String getType() {
        return type;
    }

    /**
     *
     * @param type The Type
     */
    @JsonProperty("Type")
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return The Remote URL
     */
    @JsonProperty("Remote URL")
    public String getRemoteUrl() {
        return remoteUrl;
    }

    /**
     *
     * @param remoteUrl The Remote URL
     */
    @JsonProperty("Remote URL")
    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    /**
     *
     * @return The size
     */
    @JsonProperty("Size")
    public String getSize() {
        return size;
    }

    /**
     *
     * @param size The Size
     */
    @JsonProperty("Size")
    public void setSize(String size) {
        this.size = size;
    }

    /**
     *
     * @return The driver
     */
    @JsonProperty("Driver")
    public String getDriver() {
        return driver;
    }

    /**
     *
     * @param driver The Driver
     */
    @JsonProperty("Driver")
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     *
     * @return The userAgent
     */
    @JsonProperty("UserAgent")
    public String getUserAgent() {
        return userAgent;
    }

    /**
     *
     * @param userAgent The UserAgent
     */
    @JsonProperty("UserAgent")
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

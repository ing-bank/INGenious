
package com.ing.datalib.settings.testmgmt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "module",
    "options"
})
public class TestMgModule {

    @JsonProperty("module")
    private String module;
    @JsonProperty("options")
    private List<Option> options = new ArrayList<Option>();

    public TestMgModule() {
    }

    public TestMgModule(String module) {
        this.module = module;
    }

    /**
     *
     * @return The module
     */
    @JsonProperty("module")
    public String getModule() {
        return module;
    }

    /**
     *
     * @param module The module
     */
    @JsonProperty("module")
    public void setModule(String module) {
        this.module = module;
    }

    /**
     *
     * @return The options
     */
    @JsonProperty("options")
    public List<Option> getOptions() {
        return options;
    }

    /**
     *
     * @param options The options
     */
    @JsonProperty("options")
    public void setOptions(List<Option> options) {
        this.options = options;
    }

}

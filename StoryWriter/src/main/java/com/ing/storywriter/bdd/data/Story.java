
package com.ing.storywriter.bdd.data;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import static java.util.regex.Pattern.compile;
import org.json.simple.JSONObject;
import static com.ing.storywriter.bdd.data.DS.LN;

/**
 *
 */
public class Story {

    public String type = "Feature", name, desc;
    Map<String, Object> meta;

    String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
        update();
    }

    public Story(String name) {
        this(name, "");
    }

    public Story(String name, String desc) {
        this.name = name;
        this.desc = name;
        meta = new LinkedHashMap();
        data = getDefData(name, desc);
    }

    public Map meta() {
        return meta;
    }

    public void addMeta(Map meta) {
        if (meta != null) {
            this.meta.putAll(meta);
        }
    }

    @Override
    public String toString() {
        return "Feature: " + name + LN + desc + LN + data;
    }

    public JSONObject toJSON() {
        JSONObject js = new JSONObject();
        js.put("type", "Feature");
        js.put("name", name);
        js.put("desc", desc);
        js.put("data", data);
        return js;
    }

    private String getDefData(String name, String desc) {
        return format("#BDD-Feature File\n\nFeature: %s\n%s", name, desc);
    }

    public void update() {
        if (data.startsWith("Feature:")) {
            data = lineSeparator() + data;
        }
        Matcher m = compile(".*Feature: (.*)\n.*").matcher(data);
        while (m.find()) {
            name = m.group(1);
        }
    }
}

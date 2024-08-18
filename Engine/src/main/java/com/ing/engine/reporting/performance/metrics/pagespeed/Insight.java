
package com.ing.engine.reporting.performance.metrics.pagespeed;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * page speed insights helper object with validation and rules
 *
 * 
 * @param <K> key
 * @param <V> value
 * 
 */
@SuppressWarnings("unchecked")
public class Insight<K, V> extends JSONObject {

    private static final long serialVersionUID = 1L;

    public Insight(String name, int score) {
        put("name", name);
        put("score", score);
    }

    public void desc(String desc) {
        put("desc", desc);
    }

    public void addDescData(String data) {
        if (this.get("data") == null) {
            put("data", new JSONArray());
        }
        ((JSONArray) this.get("data")).add(parseData(data));

    }

    /**
     * check the data toe url and comments if comments exists add it as separate
     * entry
     *
     * @param dataS data to parse
     * @return data entry
     */
    public Object parseData(String dataS) {
        Matcher m = Pattern.compile("(.*) \\((.*)\\)").matcher(dataS);

        JSONObject data = new JSONObject();
        if (m.matches()) {
            data.put("url", m.group(1));
            data.put("comments", m.group(2));
        } else {
            data.put("url", dataS);
        }

        return data;
    }

}


package com.ing.ide.util.data;

import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * 
 */
public class JSONProvider {

    public static String getJSONString(Map<String, String> map) {
        return JSONObject.toJSONString(map);
    }

    public static Map<String, String> getJSONMap(String json) {        
        return (Map<String, String>) JSONValue.parse(json);
    }

}


package com.ing.engine.util.data;

import com.ing.engine.constants.FilePath;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * 
 */
public class KeyMap {

    public static final Pattern CONTEXT_VARS = Pattern.compile("\\{(.+?)\\}");
    public static final Pattern ENV_VARS = Pattern.compile("\\$\\{(.+?)\\}");
    public static final Pattern USER_VARS = Pattern.compile("%(.+?)%");
    
    private static Map<Object,Object> systemVars;
    
    public static Map<Object, Object> getSystemVars(){
        if(systemVars==null){
            systemVars=new HashMap<>();
            systemVars.put("app.lib", FilePath.getLibPath());
            systemVars.put("app.root", FilePath.getAppRoot());
            systemVars.put("app.config", FilePath.getConfigurationPath()); 
            systemVars.putAll(System.getProperties());  
            systemVars.putAll(System.getenv());
        }
        return systemVars;
    }
    public static String resolveContextVars(String in, Map<?,?> vMap) {
        return replaceKeys(in, CONTEXT_VARS, true, 1, vMap);
    }

    public static String resolveEnvVars(String in) {
        return replaceKeys(in, ENV_VARS);
    }

    /**
     * replace the given pattern with the key-map value
     *
     * @param in input string
     * @param pattern pattern to match
     * @param preserveKeys true to preserve key pattern if its not in key-map
     * @param passes no times to resolve
     * <br> n for n- level of keys (level -> keys inside keys)
     * @param maps key-map list
     * @return resolved string
     */
    public static String replaceKeys(String in, Pattern pattern, boolean preserveKeys, int passes, Map<?,?>... maps) {
        String out = in;
        for (int pass = 1; pass <= passes; pass++) {
            Matcher m = pattern.matcher(in);
            String match, key;
            while (m.find()) {
                match = m.group();
                key = m.group(1);
                Boolean resolved = false;
                if (maps != null) {
                    for (Map<?, ?> map : maps) {
                        if ((resolved = map.containsKey(key))) {
                            out = out.replace(match, Objects.toString(map.get(key)));
                            break;
                        }
                    }
                }
                if (!resolved && !preserveKeys) {
                    out = out.replace(match, key);
                }
            }
            in=out;
        }
        return out;
    }

    /**
     *
     * @param in input string
     * @param p pattern to match
     * @return resolved string
     */
    public static String replaceKeys(String in, Pattern p) {
        String properties = System.getProperties().toString().replace("{", "").replace("}", "");
        Map <String,String> props = new HashMap<String, String>();
        for (String prop : properties.split(",")){
          if(prop.split("=").length == 2)
          props.put(prop.split("=")[0].trim(), prop.split("=")[1].trim());
        }
        String environmentVars = System.getenv().toString().replace("{", "").replace("}", "");
        Map <String,String> envs = new HashMap<String, String>();
        for (String env : environmentVars.split(",")){
          if(env.split("=").length == 2)
          envs.put(env.split("=")[0].trim(), env.split("=")[1].trim());
        }
        return replaceKeys(in, p, false, 1, props, envs);
    }

    public static String resolveSystemVars(String in) {
       return replaceKeys(in, CONTEXT_VARS, true, 1, getSystemVars());
    }

}

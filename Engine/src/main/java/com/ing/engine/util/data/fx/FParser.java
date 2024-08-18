
package com.ing.engine.util.data.fx;

/**
 *
 * 
 */
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class FParser {

    private static final Logger LOG = Logger.getLogger(FParser.class.getName());
    private final static String RX = "(?x),(?=([^\"] * \" [^\"] * \" )* [^\"] * $ )";

    public static List<String> FUNCTIONS;
    private static ScriptEngine JS;
    private static Class<?> FX;

    static {
        init();
    }

    public static List<String> getFuncList() {
        return FUNCTIONS;
    }

    /**
     * eval expressions using custom parser and Functions from {@link Functions}
     *
     * @param s expression
     * @return result
     */
    public static Object eval(String s) {

        String func = FN(s);
        s = s.replaceFirst(func, "");
        String[] params = {};
        if (s.length() >= 2) {
            String param = s.substring(1, s.lastIndexOf(")"));
            params = param.split(RX, -1);
        }
        return String.valueOf(EXE(func, params));
    }

    /**
     * eval expressions using javascript engine
     *
     * @param script expression/script to eval.
     * @return result
     */
    public static String evaljs(String script) {
        try {
            return JS.eval("JSON.stringify(" + script + ")").toString();
        } catch (ScriptException ex) {
           LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return "undefined";
    }

    private static void init() {
        JS = getJSEngine();
        try {
            FX = getClazz();
            FUNCTIONS = new ArrayList<>();
            for (Method m : FX.getDeclaredMethods()) {
                FUNCTIONS.add(m.getName());
            }
        } catch (Exception ex) {
           LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static ScriptEngine getJSEngine() {
        try {
            ScriptEngine js = new ScriptEngineManager().getEngineByName("JavaScript");
            js.eval("Object.getOwnPropertyNames(Math).map(function(p){this[p]=Math[p]});");
            return js;
        } catch (Exception ex) {
           LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    private static Object EXE(String func, String[] params) {
        try {
            List<String> p = new ArrayList<>();
            for (String param : params) {
                p.add(RES(param));
            }
            Method actn = FX.getDeclaredMethod(func, String[].class);
            return actn.invoke(FX.newInstance(),
                    new Object[]{p.toArray(new String[params.length])});

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    private static Class<?> getClazz() throws ClassNotFoundException {
        return Class.forName(Functions.class.getName());
    }

    private static String RES(String arg) {
        arg = remQ(arg);
        if (arg.startsWith("=")) {
            if (isF(FN(arg.substring(1)))) {
                return String.valueOf(eval(arg.substring(1)));
            }
        }
        return arg;
    }

    private static String FN(String s) {
        return s.substring(0, s.indexOf("("));
    }

    private static boolean isF(String arg) {
        try {
            Method m = FX.getDeclaredMethod(arg, String[].class);
            return m != null;
        } catch (NoSuchMethodException | SecurityException ex) {
           LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }
    }

    private static String remQ(String arg) {
        if (arg.startsWith("\"") && arg.endsWith("\"")) {
            return arg.substring(1, arg.lastIndexOf("\""));
        } else {
            return arg;
        }
    }

}

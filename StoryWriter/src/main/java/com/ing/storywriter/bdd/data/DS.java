
package com.ing.storywriter.bdd.data;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
public class DS {

    public static String[] STEPTYPES = {"Given", "And", "When", "Then","But"};
    public static Set<String> STEPDIC = new LinkedHashSet();
    public static Set<String> VARSET = new LinkedHashSet();
    public static String LN = "\n";
    public static String TAB = "\t";
    public static String GSTORIES = "GivenStories";
    public static String META = "Meta";

    static {
        STEPDIC.addAll(Arrays.asList(new String[]{}));
    }

    public  static synchronized void update(String... vals) {
        if (STEPDIC.size() < 500 && vals != null && vals.length > 0) {
            for (String v : vals) {
                if (v != null && v.length() > 3) {
                    if (v.startsWith("<") && v.endsWith(">")) {
                        VARSET.add(v);
                    } else {
                        STEPDIC.add(v);
                    }
                }
            }
        }

    }

    public static synchronized void updateV(String... vals) {
        for (String v : vals) {
            if (v != null && v.length() > 3) {
                VARSET.add("<" + v + ">");
            }
        }

    }
}

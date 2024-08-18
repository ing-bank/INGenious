
package com.ing.storywriter.util;

/**
 *
 */
public class SLogger {

    public static boolean debug = true;

    public static void LOG(String HD, String MSG) {
        if (debug) {
            System.out.println("[" + HD + " : " + MSG + " ]");
        }
    }

    public static void LOGI(String MSG) {
        LOG("INFO ", MSG);
    }

    public static void LOGE(String MSG) {
        LOG("ERROR", MSG);
    }

    public static void LOGA(String MSG) {
        Notification.show(MSG);
    }
}

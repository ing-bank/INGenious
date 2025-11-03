package com.ing.engine.commands.ssh;
import com.jcraft.jsch.Logger;

public class SSHLogger implements Logger {
    public boolean isEnabled(int level) {
        // Enable all levels of logging
        return true;
    }

    public void log(int level, String message) {
        String levelStr;
        switch (level) {
            case Logger.DEBUG: levelStr = "DEBUG"; break;
            case Logger.INFO: levelStr = "INFO"; break;
            case Logger.WARN: levelStr = "WARN"; break;
            case Logger.ERROR: levelStr = "ERROR"; break;
            case Logger.FATAL: levelStr = "FATAL"; break;
            default: levelStr = "UNKNOWN"; break;
        }
        System.out.println("[" + levelStr + "] " + message);
    }
}

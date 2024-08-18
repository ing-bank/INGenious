
package com.ing.ide.util;

/**
 *
 * 
 */
public class SystemInfo {

    private static final String[] OS_INFO = {"os.name", "os.version", "os.arch"},
            OS_INFO_NAME = {"OS Name         : ",
                "OS Version      : ",
                "OS Architecture : "},
            JAVA_INFO = {"java.vm.version", "java.home", "java.version", "java.runtime.version", "java.class.path", "sun.io.unicode.encoding"},
            FILE_SYS_INFO = {"user.dir", "file.separator", "path.separator"},
            JAVA_INFO_NAME = {"Java VM Version      : ",
                "Java Home            : ",
                "Java Version         : ",
                "Java Runtime Version : ",
                "Java ClassPath       : ",
                "IO Unicode Encoding  : "},
            FIEL_SYS_INFO_NAME = {"User Directory : ",
                "File Separator : ",
                "Path Separator : "};

   private SystemInfo(){
       
   }

    public static String getOSInfo() {
        String info = "";
        int index = 0;
        for (String key : OS_INFO) {
            info += OS_INFO_NAME[index++] + System.getProperty(key) + System.lineSeparator();
        }

        return info;
    }

    public static String getFileSystemInfo() {
        String info = "";
        int index = 0;
        for (String key : FILE_SYS_INFO) {
            info += FIEL_SYS_INFO_NAME[index++] + System.getProperty(key) + System.lineSeparator();
        }

        return info;
    }

    public static String getJavaInfo() {
        String info = "";
        int index = 0;
        for (String key : JAVA_INFO) {
            info += JAVA_INFO_NAME[index++] + System.getProperty(key) + System.lineSeparator();
        }

        return info;
    }

    public static boolean isWindows() {
        return System.getProperty(OS_INFO[0]).toLowerCase().contains("windows");
    }
     public static boolean osx() {
        return System.getProperty(OS_INFO[0]).toLowerCase().contains("mac");
    }

}

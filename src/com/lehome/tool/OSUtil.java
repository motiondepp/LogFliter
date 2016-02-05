package com.lehome.tool;

/**
 * Created by legendmohe on 16/1/24.
 */
public class OSUtil {
    private static String OS;

    static {
        OS = System.getProperty("os.name");
    }

    public static String getOsName() {
        return OS;
    }

    public static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }

}

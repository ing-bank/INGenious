package com.example.plugin;

public class SamplePlugin {
    /**
     * Static state a plugin would keep between lookups. Two lookups that see two copies of this
     * field are two copies of the class, which is what the class loader cache prevents.
     */
    public static String sharedHandle;
}


package com.ing.engine.support.reflect;

import com.ing.engine.core.CommandControl;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

public class MethodExecutor {
    
    private static final Map<String, MethodHandle> CACHE = new HashMap<>();
    private static final Map<MethodHandle, Class<?>> CACHE_CLASS = new HashMap<>();

    public static void init() {
        CACHE.clear();
        CACHE_CLASS.clear();
        Discovery.discoverCommands();
    }
    
    public static boolean executeMethod(String mName, CommandControl inst) throws Throwable {
        MethodHandle handle = getHandle(mName);
        if (handle != null) {
            handle.invoke(CACHE_CLASS.get(handle).getConstructor(
                    CommandControl.class).newInstance(inst));
            
            return true;
        }
        return false;
    }
    
    private static MethodHandle makeHandle(String mName) {
        for (Class<?> c : Discovery.getClassList()) {
            MethodHandle handle = getHandle(c, mName);
            if (handle != null) {
                CACHE.put(mName, handle);
                CACHE_CLASS.put(handle, c);
                return handle;
            }
        }
        return null;
    }
    
    private static MethodHandle getHandle(Class<?> c, String mName) {
        try {
            return MethodHandles.lookup().findVirtual(c, mName,
                    MethodType.methodType(void.class
                    ));
        } catch (Exception ex) {
            return null;
        }
    }
    
    private static boolean cached(String mName) {
        return CACHE.containsKey(mName) && CACHE_CLASS.containsKey(CACHE.get(mName));
    }
    
    private static MethodHandle getHandle(String mName) {
        if (cached(mName)) {
            return CACHE.get(mName);
        } else {
            return makeHandle(mName);
        }
    }
}

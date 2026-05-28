
package com.ing.engine.support.reflect;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.contract.DatabasePluginApi;
import com.ing.engine.commands.database.General;
import com.ing.ingenious.api.contract.BrowserPluginApi;
import com.ing.ingenious.api.contract.MobilePluginApi;
import com.ing.ingenious.api.contract.CommandPluginApi;

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
    
    /**
     * Dynamically executes a method by name on a discovered command class, injecting the appropriate constructor argument.
     * <p>
     * <b>Dynamic Invocation Logic:</b>
     * <ul>
     *   <li>Looks up a MethodHandle for the given method name using the command discovery cache.</li>
     *   <li>Attempts to instantiate the target class using one of the following constructors (in order):</li>
     *   <ol>
     *     <li>{@code (GeneralBrApi)}: If present, injects a new {@link com.ing.engine.commands.browser.General} instance.</li>
     *     <li>{@code (GeneralDbApi)}: If present, injects a new {@link com.ing.engine.commands.database.General} instance.</li>
     *     <li>{@code (GeneralMobileApi)}: If present, injects a new mobile General instance.</li>
     *     <li>{@code (CommandControl)}: Fallback, injects the provided {@code inst} argument.</li>
     *   </ol>
     *   <li>Invokes the discovered method on the constructed instance.</li>
     * </ul>
     *
     * @param mName the name of the method to execute
     * @param inst the {@link CommandControl} instance to inject if required
     * @return true if the method was found and executed, false otherwise
     * @throws Throwable if method invocation or instantiation fails
     */
    public static boolean executeMethod(String mName, CommandControl inst) throws Throwable {
        MethodHandle handle = getHandle(mName);
        System.out.println("Executing method: " + mName);
        if (handle != null) {
            Class<?> clazz = CACHE_CLASS.get(handle);
            Object instance = createInstance(clazz, inst);
            handle.invoke(instance);
            return true;
        }
        return false;
    }

    private static Object createInstance(Class<?> clazz, CommandControl inst) throws Exception {
        // Try BrowserPluginApi constructor
        try {
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(com.ing.ingenious.api.contract.BrowserPluginApi.class);
            com.ing.ingenious.api.contract.BrowserPluginApi genBr = new com.ing.engine.commands.browser.General(inst);
            return ctor.newInstance(genBr);
        } catch (NoSuchMethodException ignored) {}
        
        // Try DatabasePluginApi constructor
        try {
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(com.ing.ingenious.api.contract.DatabasePluginApi.class);
            com.ing.ingenious.api.contract.DatabasePluginApi genDb = new General(inst);
            return ctor.newInstance(genDb);
        } catch (NoSuchMethodException ignored) {}
        
        // Try MobilePluginApi constructor
        try {
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(com.ing.ingenious.api.contract.MobilePluginApi.class);
            com.ing.ingenious.api.contract.MobilePluginApi genMobile = new com.ing.engine.commands.mobile.MobileGeneral(inst);
            return ctor.newInstance(genMobile);
        } catch (NoSuchMethodException ignored) {}

        // Try WebservicePluginApi constructor
        try {
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(com.ing.ingenious.api.contract.WebservicePluginApi.class);
            com.ing.ingenious.api.contract.WebservicePluginApi genWebservice = new com.ing.engine.commands.webservice.GeneralWebservice(inst);
            return ctor.newInstance(genWebservice);
        } catch (NoSuchMethodException ignored) {}
        
        // Try CommandPluginApi constructor (generic plugin API)
        try {
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(com.ing.ingenious.api.contract.CommandPluginApi.class);
            com.ing.ingenious.api.contract.CommandPluginApi genCmd = new com.ing.engine.commands.browser.Command(inst);
            return ctor.newInstance(genCmd);
        } catch (NoSuchMethodException ignored) {}
        
        // Fallback to CommandControl constructor
        java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(CommandControl.class);
        return ctor.newInstance(inst);
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

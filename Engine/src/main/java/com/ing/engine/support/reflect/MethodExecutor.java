
package com.ing.engine.support.reflect;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.contract.GeneralDbApi;
import com.ing.engine.commands.database.General;
import com.ing.ingenious.api.contract.GeneralBrApi;

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
            Object arg;
            java.lang.reflect.Constructor<?> ctor;
            try {
                ctor = clazz.getConstructor(com.ing.ingenious.api.contract.GeneralBrApi.class);
                com.ing.ingenious.api.contract.GeneralBrApi GenDb = new com.ing.engine.commands.browser.General(inst);
                arg = (com.ing.ingenious.api.contract.GeneralBrApi) GenDb;
            } catch (NoSuchMethodException e) {
               try {
                    ctor = clazz.getConstructor(com.ing.ingenious.api.contract.GeneralDbApi.class);
                    com.ing.ingenious.api.contract.GeneralDbApi GenDb = new General(inst);
                    arg = (com.ing.ingenious.api.contract.GeneralDbApi) GenDb;
                } catch (NoSuchMethodException e2) {
                    ctor = clazz.getConstructor(CommandControl.class);
                    arg = inst;
                }
            }
            handle.invoke(ctor.newInstance(arg));
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


package com.ing.engine.support.reflect;

import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.drivers.MobileDriver;
import com.ing.engine.drivers.PlaywrightDriver;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

public class MethodExecutor {
    
    private static final Map<String, MethodHandle> CACHE = new HashMap<>();
    private static final Map<MethodHandle, Class<?>> CACHE_CLASS = new HashMap<>();
    public MobileDriver mobileDriver;
    public static void init() {
        CACHE.clear();
        CACHE_CLASS.clear();
        Discovery.discoverCommands();
    }
    
    public static boolean executeMethod(String mName, CommandControl inst) throws Throwable {
          System.out.println("**** Inside MethodExecutor executeMethod mName  "+mName);
        MethodHandle handle = getHandle(mName);
        System.out.println("**** Inside MethodExecutor handle "+handle);
        System.out.println("**** Inside MethodExecutor inst "+inst);
        if (handle != null) {
            System.out.println("**** Inside MethodExecutor executeMethod if  ");
            handle.invoke(CACHE_CLASS.get(handle).getConstructor(
                    CommandControl.class).newInstance(inst));
            System.out.println("**** END Inside MethodExecutor executeMethod if  ");
            return true;
        }
        return false;
    }
    
//    private static MethodHandle makeHandle(String mName) {
//        for (Class<?> c : Discovery.getClassList()) {
//             System.out.println("** $$ C name : " +c.getName());
//              System.out.println("** $$ m name : " +mName);
//              MethodHandle handle = getHandle(c, mName);
//            PlaywrightDriver a=new PlaywrightDriver();
//            if(a.isMobileExecution())
//            {
//                System.out.println("** $$ ----- Make Handle handle else : " +mName);
//                if(c.getName().contains(".MobileCommands"))
//                {
//                      System.out.println("** $$ -----  !!! Make Handle handle else : " );
//                if (handle != null ) {
//               
//                 System.out.println("** $$ inside of Make Handle handle ");
//                CACHE.put(mName, handle);
//                CACHE_CLASS.put(handle, c);
//                return handle;
//                }
//            } 
//            }
//            else
//            {
//            System.out.println("** $$ Make Handle handle else : " +mName);
//            if (handle != null ) {
//               
//                 System.out.println("** $$ inside of Make Handle handle ");
//                CACHE.put(mName, handle);
//                CACHE_CLASS.put(handle, c);
//                return handle;
//            }
//        }
//        }
//        return null;
//    }
    
        private static MethodHandle makeHandle(String mName) {
        for (Class<?> c : Discovery.getClassList()) {
             System.out.println("** $$ C name : " +c.getName());
              System.out.println("** $$ m name : " +mName);
              MethodHandle handle = getHandle(c, mName);

            System.out.println("** $$ Make Handle handle else : " +mName);
            if (handle != null ) {
               
                 System.out.println("** $$ inside of Make Handle handle ");
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

         for (String name: CACHE.keySet()) {
    String key = name.toString();
    String value = CACHE.get(name).toString();
    System.out.println("** $$ Cache : " +key + " : " + value);
}        
                  for (MethodHandle cname: CACHE_CLASS.keySet()) {
    String ckey = cname.toString();
    String cvalue = CACHE.get(cname).toString();
    System.out.println("** $$ Cache Class : " +ckey + " : " + cvalue);
}
        return CACHE.containsKey(mName) && CACHE_CLASS.containsKey(CACHE.get(mName));
    }
    
    private static MethodHandle getHandle(String mName) {
        if (cached(mName)) {
            System.out.println("****$$ Inside MethodHandler if");
            return CACHE.get(mName);
        } else {
            System.out.println("****$$ Inside MethodHandler else");
            return makeHandle(mName);
        }
    }
}

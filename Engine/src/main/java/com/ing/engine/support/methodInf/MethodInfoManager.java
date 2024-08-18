
package com.ing.engine.support.methodInf;

import com.ing.datalib.component.TestStep;
import com.ing.engine.support.AnnontationUtil;
import com.ing.engine.support.reflect.Discovery;
import com.ing.engine.support.reflect.MethodExecutor;
import eu.infomas.annotation.AnnotationDetector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class MethodInfoManager {
    
    public static Map<String, Action> methodInfoMap = new HashMap<>();
    
    private static final AnnotationDetector.MethodReporter METHOD_REPORTER = new AnnotationDetector.MethodReporter() {
        
        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Annotation>[] annotations() {
            return new Class[]{Action.class};
        }
        
        @Override
        public void reportMethodAnnotation(Class<? extends Annotation> annotation,
                String className, String methodName) {
            loadMethod(className, methodName);
        }
        
    };
    
    private static final AnnotationDetector ANNOTATION_DETECTOR = new AnnotationDetector(METHOD_REPORTER);
    
    public static void load() {
        MethodExecutor.init();
        methodInfoMap.clear();
        AnnontationUtil.detect(ANNOTATION_DETECTOR, "com.ing.engine.commands");
    }
    
    private static void loadMethod(String className, String methodName) {
        try {
            Method method = getClass(className).getMethod(methodName);
            Action mInfo = method.getAnnotation(Action.class);
            methodInfoMap.put(methodName, mInfo);
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(MethodInfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static Class<?> getClass(String className) {
        try {
            Class<?> class_ = Discovery.getClassByName(className);
            if (class_ != null) {
                return class_;
            }
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MethodInfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static List<String> getMethodListFor(ObjectType type, ObjectType... others) {
        List<String> methodList = new ArrayList<>();
        for (Map.Entry<String, Action> entry : methodInfoMap.entrySet()) {
            String methodName = entry.getKey();
            Action mInfo = entry.getValue();
            if (mInfo.object().equals(type)
                    || (others != null
                    && Arrays.asList(others).contains(mInfo.object()))) {
                methodList.add(methodName);
            }
        }
        Collections.sort(methodList);
        return methodList;
    }
    
    public static String getDescriptionFor(String action) {
        if (methodInfoMap.containsKey(action)) {
            return methodInfoMap.get(action).desc();
        }
        return "";
    }
    
    public static String getResolvedDescriptionFor(TestStep step) {
        return getDescriptionFor(step.getAction())
                .replace("[<Object>]", step.getObject())
                .replace("[<Object2>]", step.getCondition())
                .replace("[<Data>]", step.getInput());
    }
    
}

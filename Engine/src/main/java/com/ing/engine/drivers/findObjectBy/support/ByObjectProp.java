
package com.ing.engine.drivers.findObjectBy.support;

import com.ing.engine.support.AnnontationUtil;
import eu.infomas.annotation.AnnotationDetector;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.By;

public class ByObjectProp {

    private static ByObjectProp byObjectProp = new ByObjectProp();

    private final List<ObjectPropClass> OBJ_PROP_CLASSES = new ArrayList<>();

    private static final AnnotationDetector.MethodReporter METHOD_REPORTER = new AnnotationDetector.MethodReporter() {

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Annotation>[] annotations() {
            return new Class[]{SProperty.class};
        }

        @Override
        public void reportMethodAnnotation(Class<? extends Annotation> annotation,
                String className, String methodName) {
            byObjectProp.load(className, methodName);
        }

    };
    private static final AnnotationDetector ANNOTATION_DETECTOR = new AnnotationDetector(METHOD_REPORTER);

    public static void load() {
        if (byObjectProp.OBJ_PROP_CLASSES.isEmpty()) {
            AnnontationUtil.detect(ANNOTATION_DETECTOR, "com.ing.engine.drivers.findObjectBy");
        }
    }

    public static ByObjectProp get() {
        return byObjectProp;
    }

    private void load(String className, String methodName) {
        getObjPropClass(className).loadMethod(methodName);
    }

    private ObjectPropClass getObjPropClass(String className) {
        for (ObjectPropClass objectPropClass : OBJ_PROP_CLASSES) {
            if (objectPropClass.className.equals(className)) {
                return objectPropClass;
            }
        }
        ObjectPropClass objPropClass = new ObjectPropClass(className);
        OBJ_PROP_CLASSES.add(objPropClass);
        return objPropClass;
    }

    public List<String> getMethods() {
        List<String> methods = new ArrayList<>();
        for (ObjectPropClass objectPropClass : OBJ_PROP_CLASSES) {
            methods.addAll(objectPropClass.propertyMethodMap.keySet());
        }
        return methods;
    }

    public By getBy(String propertyName, String value) {
        value = Objects.toString(value, "");
        for (ObjectPropClass objectPropClass : OBJ_PROP_CLASSES) {
            if (objectPropClass.propertyMethodMap.containsKey(propertyName)) {
                Method method = objectPropClass.propertyMethodMap.get(propertyName);
                try {                   
                    return (By) method.invoke(objectPropClass.classObject, value);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    Logger.getLogger(ByObjectProp.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            }
        }
        if(!propertyName.equals("NLP_locator"))
          Logger.getLogger(ByObjectProp.class.getName()).log(Level.SEVERE, "Find logic not implemented for - {0}", propertyName);
        return null;
    }

    class ObjectPropClass {

        Class<?> aclass;

        String className;

        Object classObject;

        Map<String, Method> propertyMethodMap = new HashMap<>();

        public ObjectPropClass(String className) {
            this.className = className;
            try {
                aclass = Class.forName(className);
                classObject = aclass.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(ObjectPropClass.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void loadMethod(String methodName) {
            try {
                Method method = aclass.getMethod(methodName, String.class);
                String mName = method.getAnnotation(SProperty.class).name();
                if (!propertyMethodMap.containsKey(mName)) {
                    propertyMethodMap.put(mName, method);
                }
            } catch (NoSuchMethodException | SecurityException ex) {
                Logger.getLogger(ObjectPropClass.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

}

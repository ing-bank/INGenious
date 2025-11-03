package com.ing.engine.commands.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class GenericJsonBuilder<T> extends General {

    public GenericJsonBuilder(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.JSON, desc = "Builder Create JSON", input = InputType.NO, condition = InputType.NO)
    public void builderCreateJsonBuilder() {
        try {
            factory.computeIfAbsent(Thread.currentThread().toString(), k -> JsonNodeFactory.instance);
            Report.updateTestLog(Action, "JSON Builder Created", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error initializing JSON Builder", Status.FAIL);
            throw new RuntimeException("Error initializing JSON Builder", e);
        }
    }

    @Action(object = ObjectType.JSON, desc = "Builder Create Object", input = InputType.YES, condition = InputType.NO)
    public void builderCreateObject() {
        try {
            pendingKeys.computeIfAbsent(Thread.currentThread().toString(), k -> new ArrayList<>()).add(Data);
            isArrayPending.computeIfAbsent(Thread.currentThread().toString(), k -> new ArrayList<>()).add(false);
            Report.updateTestLog(Action, "JSON Object Created", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error Creating Object", Status.FAIL);
            throw new RuntimeException("Error Creating Object", e);
        }
    }

    @Action(object = ObjectType.JSON, desc = "Builder Create Child Object", input = InputType.YES, condition = InputType.NO)
    public void builderCreateChildObject() {
        try {
            pendingKeys.get(Thread.currentThread().toString()).add(Data);
            isArrayPending.get(Thread.currentThread().toString()).add(false);
            Report.updateTestLog(Action, "JSON Child Object Created", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error Creating Child Object", Status.FAIL);
            throw new RuntimeException("Error Creating Child Object", e);
        }
    }

    @Action(object = ObjectType.JSON, desc = "Builder Create Sibling Object", input = InputType.YES, condition = InputType.NO)
    public void builderCreateSibling() {
        try {
            builderJSONEndElement(); // close previous sibling
            pendingKeys.get(Thread.currentThread().toString()).add(Data);
            isArrayPending.get(Thread.currentThread().toString()).add(false);
            Report.updateTestLog(Action, "JSON Sibling Object Created", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error Creating Sibling Object", Status.FAIL);
            throw new RuntimeException("Error Creating Sibling Object", e);
        }
    }

    @Action(object = ObjectType.JSON, desc = "Builder Create Array Object", input = InputType.YES, condition = InputType.NO)
    public void builderCreateArray() {
        try {
            pendingKeys.get(Thread.currentThread().toString()).add(Data);
            isArrayPending.get(Thread.currentThread().toString()).add(true);
            currentArrayKey.computeIfAbsent(Thread.currentThread().toString(), k -> Data);
//            currentArrayKey = Data;
            Report.updateTestLog(Action, "JSON Array Object Created", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error Creating Array Object", Status.FAIL);
            throw new RuntimeException("Error Creating Array Object", e);
        }
    }

    @Action(object = ObjectType.JSON, desc = "Builder Start Property", input = InputType.YES, condition = InputType.NO)
    public void builderSetProperty() {
        try {
            currentPropertyKey.put(Thread.currentThread().toString(), Data);
            Report.updateTestLog(Action, "JSON Property Set", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error Setting Property", Status.FAIL);
            throw new RuntimeException("Error Setting Property", e);
        }
    }

    @Action(object = ObjectType.JSON, desc = "Builder Set Property Value", input = InputType.YES, condition = InputType.NO)
    public void builderSetPropertyValue() {
        try {
            ensurePendingAttached();
            ObjectNode current = getCurrentObject();
            current.putPOJO(currentPropertyKey.get(Thread.currentThread().toString()), parseValue(Data));
            markUsed();
            Report.updateTestLog(Action, "JSON Property Value Set", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error Setting Property Value", Status.FAIL);
            throw new RuntimeException("Error Setting Property Value", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T parseValue(String input) {
        try {
            return (T) Integer.valueOf(input);
        } catch (NumberFormatException e1) {
            try {
                return (T) Double.valueOf(input);
            } catch (NumberFormatException e2) {
                if ("true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)) {
                    return (T) Boolean.valueOf(input);
                }
                return (T) input; // fallback to String
            }
        }
    }

    private void markUsed() {
        if (!jsonUsedStack.get(Thread.currentThread().toString()).isEmpty()) {
            jsonUsedStack.get(Thread.currentThread().toString()).set(jsonUsedStack.get(Thread.currentThread().toString()).size() - 1, true);
        }
    }

    @Action(object = ObjectType.JSON, desc = "Builder Add Array Element", input = InputType.NO, condition = InputType.NO)
    public void builderAddArrayElement() {
        try {
            ensurePendingAttached();
            ObjectNode current = getCurrentObject();
            ArrayNode array = (ArrayNode) current.get(currentArrayKey.get(Thread.currentThread().toString()));
            if (array == null) {
                array = factory.get(Thread.currentThread().toString()).arrayNode();
                current.set(currentArrayKey.get(Thread.currentThread().toString()), array);
            }

            // Create a new object for this array element
            ObjectNode newElement = factory.get(Thread.currentThread().toString()).objectNode();
            array.add(newElement);

            // Push this element onto the stack so properties can be added to it
            objectStack.get(Thread.currentThread().toString()).add(newElement);
            jsonUsedStack.get(Thread.currentThread().toString()).add(false); // Track usage for cleanup
            Report.updateTestLog(Action, "JSON Property Started", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error Starting Property", Status.FAIL);
            throw new RuntimeException("Error Starting Property", e);
        }
    }

    private void ensurePendingAttached() {
        if (pendingKeys.get(Thread.currentThread().toString()).isEmpty()) return;

        rootNode.computeIfAbsent(Thread.currentThread().toString(), k -> factory.get(Thread.currentThread().toString()).objectNode());
        objectStack.computeIfAbsent(Thread.currentThread().toString(), k -> new ArrayList<>());
        ObjectNode parent = objectStack.get(Thread.currentThread().toString()).isEmpty() ? rootNode.get(Thread.currentThread().toString()) : objectStack.get(Thread.currentThread().toString()).get(objectStack.get(Thread.currentThread().toString()).size() - 1);

        for (int i = 0; i < pendingKeys.get(Thread.currentThread().toString()).size(); i++) {
            String key = pendingKeys.get(Thread.currentThread().toString()).get(i);
            boolean isArray = isArrayPending.get(Thread.currentThread().toString()).get(i);

            if (!parent.has(key)) {
                if (isArray) {
                    ArrayNode array = factory.get(Thread.currentThread().toString()).arrayNode();
                    parent.set(key, array);
                    currentArrayKey.computeIfAbsent(Thread.currentThread().toString(), k -> key);
                } else {
                    ObjectNode newObj = factory.get(Thread.currentThread().toString()).objectNode();
                    parent.set(key, newObj);

                    objectStack.computeIfAbsent(Thread.currentThread().toString(), k -> new ArrayList<>()).add(newObj);
                    jsonUsedStack.computeIfAbsent(Thread.currentThread().toString(), k -> new ArrayList<>()).add(false);

                    parent = newObj; // move down for next child
                }
            } else {
                if (!isArray && parent.get(key).isObject()) {
                    parent = (ObjectNode) parent.get(key);
                }
            }
        }

        pendingKeys.get(Thread.currentThread().toString()).clear();
        isArrayPending.get(Thread.currentThread().toString()).clear();
    }

    // -------------------------
    // End Element Logic
    // -------------------------
    @Action(object = ObjectType.JSON, desc = "Builder End Element", input = InputType.NO, condition = InputType.NO)
    public void builderJSONEndElement() {
        try {
            if (!objectStack.get(Thread.currentThread().toString()).isEmpty()) {
                int idx = objectStack.get(Thread.currentThread().toString()).size() - 1;
                ObjectNode last = objectStack.get(Thread.currentThread().toString()).get(idx);

                // Remove empty nodes recursively
                if (!jsonUsedStack.get(Thread.currentThread().toString()).get(idx)) {
                    removeIfEmpty(last);
                }

                objectStack.get(Thread.currentThread().toString()).remove(idx);
                jsonUsedStack.get(Thread.currentThread().toString()).remove(idx);
                Report.updateTestLog(Action, "JSON Element Ended", Status.DONE);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error Ending JSON Element", Status.FAIL);
            throw new RuntimeException("Error Ending JSON Element", e);
        }

    }


    private void removeIfEmpty(ObjectNode node) {
        if (!isCompletelyEmpty(node)) return;

        // Find parent and remove node if empty
        for (ObjectNode parent : objectStack.get(Thread.currentThread().toString())) {
            for (Iterator<String> it = parent.fieldNames(); it.hasNext(); ) {
                String fieldName = it.next();
                if (parent.get(fieldName).isArray()) {
                    ArrayNode array = (ArrayNode) parent.get(fieldName);
                    array.removeIf(n -> n.equals(node));

                    // ✅ If array becomes empty, remove the array field
                    if (array.size() == 0) {
                        parent.remove(fieldName);
                    }
                }
            }
        }
    }

    private boolean isCompletelyEmpty(ObjectNode node) {
        if (node.size() == 0) return true;

        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            String fieldName = it.next();
            if (node.get(fieldName).isObject()) {
                if (!isCompletelyEmpty((ObjectNode) node.get(fieldName))) {
                    return false;
                }
            } else if (node.get(fieldName).isArray()) {
                ArrayNode array = (ArrayNode) node.get(fieldName);
                for (int i = 0; i < array.size(); i++) {
                    if (array.get(i).isObject() && !isCompletelyEmpty((ObjectNode) array.get(i))) {
                        return false;
                    }
                }
            } else {
                return false; // Found a primitive property
            }
        }
        return true;
    }

    private ObjectNode getCurrentObject() {
        if (objectStack.get(Thread.currentThread().toString()).isEmpty()) {
            throw new IllegalStateException("No current object. Did you create root/child first?");
        }
        return objectStack.get(Thread.currentThread().toString()).get(objectStack.get(Thread.currentThread().toString()).size() - 1);
    }

    // -------------------------
    // Output
    // -------------------------
    @Action(object = ObjectType.JSON, desc = "Write To File", input = InputType.YES, condition = InputType.NO)
    public void builderJSONWriteToFile() {
        try {
            mapper.computeIfAbsent(Thread.currentThread().toString(), k -> new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValue(new File(Data), rootNode.get(Thread.currentThread().toString()));
            Report.updateTestLog(Action, "JSON File Saved", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error writing JSON file", Status.DONE);
            throw new RuntimeException("Error writing JSON file", e);
        }
    }

    public ObjectNode getDocument() {
        return rootNode.get(Thread.currentThread().toString());
    }

}


package com.ing.engine.commands.xml;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;

/**
 * Generic XML Builder that:
 * - Tracks element hierarchy internally (no parent arguments needed)
 * - Supports attributes and text via method arguments
 * - Skips elements that end up empty (no attributes, text, or children)
 * - Can create root automatically (first createElement call)
 */
public class GenericXmlBuilder<T> extends General {

    public GenericXmlBuilder(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.XML, desc = "Builder Create XML", input = InputType.NO, condition = InputType.NO)
    public void builderCreateXmlBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document.put(iterationContext, builder.newDocument());
            Report.updateTestLog(Action, "XML Builder Created", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Error initializing XML document", Status.FAIL);
            throw new RuntimeException("Error initializing XML document", e);
        }
    }

    /**
     * Creates a new XML element.
     * If no root exists, it becomes the root; otherwise, it's deferred.
     */
    @Action(object = ObjectType.XML, desc = "Builder Create Element", input = InputType.YES, condition = InputType.NO)
    public void builderCreateElement() {
        try {
            Element element = document.get(iterationContext).createElement(Data);
            if (document.get(iterationContext).getDocumentElement() == null) {
                // No root yet — this becomes the root
                document.get(iterationContext).appendChild(element);
                elementStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(element);
                usedStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(true);
                Report.updateTestLog(Action, "XML Builder Root Created", Status.DONE);
            } else {
                // Defer until we know it will actually be used
                pendingStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(element);
                pendingUsedStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(false);
                System.out.println("Element added to pendingStack and pendingUsedSTack: " + element);
            }
        }
        catch (Exception ex) {
            Report.updateTestLog(Action, "Error Creating XML Element", Status.FAIL);
            throw new RuntimeException("Error Creating XML Element", ex);
        }
    }

    /** Creates a new child element under the current element. */
    @Action(object = ObjectType.XML, desc = "Builder Create Child Element", input = InputType.YES, condition = InputType.NO)
    public void builderCreateChildElement() {
        try {
            ensurePendingAttached();
            Element child = document.get(iterationContext).createElement(Data);
            pendingStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(child);
            pendingUsedStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(false);
            Report.updateTestLog(Action, "Child Element Created", Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error Creating Child Element", Status.FAIL);
            throw new RuntimeException("Error Creating Child Element", ex);
        }
    }

    /** Creates a new sibling element (under the same parent). */
    @Action(object = ObjectType.XML, desc = "Builder Create Sibling Element", input = InputType.YES, condition = InputType.NO)
    public void builderCreateSiblingElement() {
        try {
            if (!pendingStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).isEmpty() && !pendingUsedStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).get(pendingUsedStack.get(iterationContext).size() - 1)) {
                pendingStack.get(iterationContext).remove(pendingStack.get(iterationContext).size() - 1);
                pendingUsedStack.get(iterationContext).remove(pendingUsedStack.get(iterationContext).size() - 1);
            } else {
                ensurePendingAttached();
                builderEndElement();
            }

            // Create the new sibling and attach it under the same parent
            Element sibling = document.get(iterationContext).createElement(Data);

            // Push sibling onto the stack
            pendingStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(sibling);
            pendingUsedStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(false);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error Creating Sibling Element", Status.FAIL);
            throw new RuntimeException("Error Creating Sibling Element", ex);
        }
    }

    /** Adds a single attribute to the current or pending element. */
    @Action(object = ObjectType.XML, desc = "Builder Add Attribute", input = InputType.YES, condition = InputType.NO)
    public void builderAddAttribute() {
        try {
            if (!Data.isEmpty()) {
                String[] parts = Data.split(",");
                markLastPendingUsed();
                ensurePendingAttached();
                Element current = getCurrentElement();
                current.setAttribute(parts[0], parts[1]);
                markUsed(current);
                Report.updateTestLog(Action, "XML Attribute added", Status.DONE);
            }
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error Adding XML Attribute", Status.FAIL);
            throw new RuntimeException("Error Adding XML Attribute", ex);
        }
    }

    @Action(object = ObjectType.XML, desc = "Builder Add Empty Attribute", input = InputType.YES, condition = InputType.NO)
    public void builderAddEmptyAttribute() {
        try {
            lastAttributeName.put(iterationContext, Data);

            Element pendingElement = getPendingElement();
            pendingElement.setAttribute(Data, "");

            Report.updateTestLog(Action, "Empty XML Attribute added with name: \"" + lastAttributeName + "\" to \"" + pendingElement.getNodeName() + "\"", Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error Adding Empty XML Attribute", Status.FAIL);
            throw new RuntimeException("Error Adding XML Empty Attribute", ex);
        }
    }

    @Action(object = ObjectType.XML, desc = "Builder Add Attribute Content", input = InputType.YES, condition = InputType.NO)
    public void builderAddAttributeContent() {
        String attributeName = lastAttributeName.getOrDefault(iterationContext, null);

        if (attributeName == null) {
            Report.updateTestLog(Action, "No attribute name was stored.", Status.FAIL);
            return;
        }

        try {
            Element pendingElement = getPendingElement();
            pendingElement.setAttribute(attributeName, Data);

            lastAttributeName.put(iterationContext, null);

            Report.updateTestLog(Action, "XML Attribute Content added", Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error Adding XML Attribute Content", Status.FAIL);
            throw new RuntimeException("Error Adding XML Attribute Content", ex);
        }
    }

    /** Adds text content to the current or pending element. */
    @Action(object = ObjectType.XML, desc = "Builder Add Text Content", input = InputType.YES, condition = InputType.NO)
    public void builderAddTextContent() {
        try {
            if (!Data.isEmpty()) {
                markLastPendingUsed();
                ensurePendingAttached();
                Element current = getCurrentElement();
                current.setTextContent(String.valueOf(Data));
                markUsed(current);
                Report.updateTestLog(Action, "XML Text Content added, element: " + current + " value: "  +  Data, Status.DONE);
            }
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error Adding XML Text Content", Status.FAIL);
            throw new RuntimeException("Error Adding XML Text Content", ex);
        }
    }

    private void markLastPendingUsed() {
        if (!pendingStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).isEmpty()) {
            pendingUsedStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).set(pendingUsedStack.get(iterationContext).size() - 1, true);
        }
    }

    /** Ends the current element. If it was empty, it will be skipped. */
    @Action(object = ObjectType.XML, desc = "Builder End Element", input = InputType.NO, condition = InputType.NO)
    public void builderEndElement() {

        try {
            if (!pendingStack.get(iterationContext).isEmpty()) {
                // If last pending never got attached, just discard it
                pendingStack.get(iterationContext).remove(pendingStack.get(iterationContext).size() - 1);
                pendingUsedStack.get(iterationContext).remove(pendingUsedStack.get(iterationContext).size() - 1);
                return;
            }
            if (!elementStack.get(iterationContext).isEmpty()) {
                int lastIndex = elementStack.get(iterationContext).size() - 1;
                Element current = elementStack.get(iterationContext).get(lastIndex);
                boolean used = usedStack.get(iterationContext).get(lastIndex);
                elementStack.get(iterationContext).remove(lastIndex);
                usedStack.get(iterationContext).remove(lastIndex);
                if (!used) {
                    // Remove from parent
                    Element parent = elementStack.get(iterationContext).isEmpty() ? null : elementStack.get(iterationContext).get(elementStack.get(iterationContext).size() - 1);
                    if (parent != null) {
                        parent.removeChild(current);
                    } else if (document.get(iterationContext).getDocumentElement() == current) {
                        document.get(iterationContext).removeChild(current);
                    }
                }
            }
            Report.updateTestLog(Action, "XML Element Ended Succesfully", Status.DONE);

        }  catch (Exception ex) {
            Report.updateTestLog(Action, "XML Element Ending Failed", Status.FAIL);
            throw new RuntimeException("XML Element Ending Failed", ex);
        }
    }

    /** Attach any pending elements that haven’t been attached yet. */
    private void ensurePendingAttached() {
        if (pendingStack.get(iterationContext).isEmpty()) return;

        boolean anyUsed = pendingUsedStack.get(iterationContext).stream().anyMatch(Boolean::booleanValue);
        if (!anyUsed && document.get(iterationContext).getDocumentElement() != null) return;

        Element parent = elementStack.get(iterationContext).isEmpty() ? null : elementStack.get(iterationContext).get(elementStack.get(iterationContext).size() - 1);

        for (int i = 0; i < pendingUsedStack.get(iterationContext).size(); i++) {
            Element next = pendingStack.get(iterationContext).get(i);
//            Element parent = elementStack.get(iterationContext).isEmpty() ? null : elementStack.get(iterationContext).get(elementStack.get(iterationContext).size() - 1);

            if (parent == null && document.get(iterationContext).getDocumentElement() == null) {
                    document.get(iterationContext).appendChild(next);
            } else if (parent != null) {
                parent.appendChild(next);
                markUsed(parent);
                parent = next;
            }

            elementStack.get(iterationContext).add(next);
            usedStack.get(iterationContext).add(false);
        }

        pendingStack.get(iterationContext).clear();
        pendingUsedStack.get(iterationContext).clear();
    }

    /** Mark the given element as used in the parallel usedStack. */
    private void markUsed(Element element) {
        int idx = elementStack.get(iterationContext).lastIndexOf(element);
        if (idx >= 0 && idx < usedStack.get(iterationContext).size()) {
            usedStack.computeIfAbsent(iterationContext, k -> new ArrayList<>()).set(idx, true);
        }
    }

    /** Get the current element (the top of the stack). */
    private Element getCurrentElement() {

       if (elementStack.get(iterationContext).isEmpty()) {
           throw new IllegalStateException("Cannot get current element.");
       }

        return elementStack.get(iterationContext).get(elementStack.get(iterationContext).size() - 1);
    }

    private Element getPendingElement() {
        if (pendingStack.get(iterationContext).isEmpty()) {
            throw new IllegalStateException("Cannot get pending element.");
        }

        return pendingStack.get(iterationContext).get(pendingStack.get(iterationContext).size() - 1);
    }

    /** Writes the XML to file, removing standalone="no". */
    @Action(object = ObjectType.XML, desc = "Builder Write To File", input = InputType.YES, condition = InputType.NO)
    public void builderWriteToFile() {
        try {
            while (!pendingStack.get(iterationContext).isEmpty()) {
                builderEndElement();
            }
        } catch (Exception ignored) {}

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            document.get(iterationContext).setXmlStandalone(true);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(document.get(iterationContext));
            StreamResult result = new StreamResult(new File(Data));
            transformer.transform(source, result);
            Report.updateTestLog(Action, "XML File Written", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "XML File Writing Failed", Status.FAIL);
            throw new RuntimeException("Error writing XML to file", e);
        }
    }

}
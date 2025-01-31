package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PageAssertions;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.options.AriaRole;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.opentest4j.AssertionFailedError;

/**
 *
 *
 */
public class Assertions extends General {

    public Assertions(CommandControl cc) {
        super(cc);
    }

    /**
     * * Assertion for 'containsText' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] contains the text [<Data>]", input = InputType.YES)
    public void assertElementContainsText() {
        String text = "";
        try {
            LocatorAssertions.ContainsTextOptions options = new LocatorAssertions.ContainsTextOptions();
            options.setTimeout(getTimeoutValue());
            text = Locator.innerHTML();
            highlightElement();
            assertThat(Locator).containsText(Data, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] contains text '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not contain text '" + Data + "'. Actual text is '" + text + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] contains the text [<Data>]", input = InputType.YES)
    public void assertElementContainsTextIgnoreCase() {
        String text = "";
        try {
            LocatorAssertions.ContainsTextOptions options = new LocatorAssertions.ContainsTextOptions();
            options.setTimeout(getTimeoutValue());
            options.setIgnoreCase(true);
            text = Locator.innerHTML();
            highlightElement();
            assertThat(Locator).containsText(Data, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] contains text '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not contain text '" + Data + "'. Actual text is '" + text + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] contains the text [<Data>]", input = InputType.YES)
    public void assertElementNotContainsText() {
        String text = "";
        try {
            LocatorAssertions.ContainsTextOptions options = new LocatorAssertions.ContainsTextOptions();
            options.setTimeout(getTimeoutValue());
            text = Locator.innerHTML();
            highlightElement();
            assertThat(Locator).not().containsText(Data, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] dos not contain text '" + Data + "'. Actual text is '" + text + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] contains text '" + Data + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'Attribute' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has Accessible Description [<Data>]", input = InputType.YES)
    public void assertElementHasAccessibleDescription() {

        try {
            LocatorAssertions.HasAccessibleDescriptionOptions options = new LocatorAssertions.HasAccessibleDescriptionOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).hasAccessibleDescription(Data, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] has Accessible Description '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not contain expected Accessible Description");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has Accessible Name [<Data>]", input = InputType.YES)
    public void assertElementHasAccessibleName() {

        try {
            LocatorAssertions.HasAccessibleNameOptions options = new LocatorAssertions.HasAccessibleNameOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).hasAccessibleName(Data, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] has Accessible Name '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not contain expected Accessible Name");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'Attribute' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has the attribute [<Data>]", input = InputType.YES)
    public void assertElementAttributeMatches() {
        String attributeName = Data.split("=")[0];
        String attributeValue = Data.split("=")[1];
        String actualattributeValue = "";
        try {
            LocatorAssertions.HasAttributeOptions options = new LocatorAssertions.HasAttributeOptions();
            options.setTimeout(getTimeoutValue());
            actualattributeValue = Locator.getAttribute(attributeName);
            highlightElement();
            assertThat(Locator).hasAttribute(attributeName, attributeValue, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] has attribute '" + attributeName + "' with value '" + attributeValue + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have attribute '" + attributeName + " = " + attributeValue + "'. Actual value is '" + actualattributeValue + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not have attribute [<Data>]", input = InputType.YES)
    public void assertElementAttributeNotMatches() {
        String attributeName = Data.split("=")[0];
        String attributeValue = Data.split("=")[1];
        String actualAttributeValue = "";
        try {
            LocatorAssertions.HasAttributeOptions options = new LocatorAssertions.HasAttributeOptions();
            options.setTimeout(getTimeoutValue());
            actualAttributeValue = Locator.getAttribute(attributeName);
            highlightElement();
            assertThat(Locator).not().hasAttribute(attributeName, attributeValue, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] does not have attribute '" + attributeName + "' with value '" + attributeValue + "'. Actual value is '" + actualAttributeValue + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has attribute '" + attributeName + " = " + actualAttributeValue + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'Class' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has class [<Data>]", input = InputType.YES)
    public void assertElementClassMatches() {
        String actualClassValue = "";
        try {
            LocatorAssertions.HasClassOptions options = new LocatorAssertions.HasClassOptions();
            options.setTimeout(getTimeoutValue());
            actualClassValue = Locator.getAttribute("class");
            highlightElement();
            assertThat(Locator).hasClass(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] has 'class' matching '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have 'class' matching '" + Data + "'. Actual value is '" + actualClassValue + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not have class [<Data>]", input = InputType.YES)
    public void assertElementClassNotMatches() {
        String actualClassValue = "";
        try {
            LocatorAssertions.HasClassOptions options = new LocatorAssertions.HasClassOptions();
            options.setTimeout(getTimeoutValue());
            actualClassValue = Locator.getAttribute("class");
            highlightElement();
            assertThat(Locator).not().hasClass(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] does not have 'class' matching '" + Data + "'. Actual value is '" + actualClassValue + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has 'class' matching '" + Data + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'Count' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if count of [<Object>] equals [<Data>]", input = InputType.YES)
    public void assertElementCountMatches() {
        int elementCount = 0;
        try {
            LocatorAssertions.HasCountOptions options = new LocatorAssertions.HasCountOptions();
            options.setTimeout(getTimeoutValue());
            elementCount = Locator.count();
            assertThat(Locator).hasCount(Integer.parseInt(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] count matches '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] count does not match '" + Data + "'. Actual count is +'" + elementCount + "'");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if count of [<Object>] does not equal [<Data>]", input = InputType.YES)
    public void assertElementCountNotMatches() {
        int elementCount = 0;
        try {
            LocatorAssertions.HasCountOptions options = new LocatorAssertions.HasCountOptions();
            options.setTimeout(getTimeoutValue());
            elementCount = Locator.count();
            assertThat(Locator).not().hasCount(Integer.parseInt(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] count does not match '" + Data + "'. Actual count is +'" + elementCount + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] count matches '" + Data + "'");
        } 
    }

    /**
     * * Assertion for 'CSS' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has the CSS [<Data>]", input = InputType.YES)
    public void assertElementCSSMatches() {
        String attributeName = Data.split("=", 2)[0];
        String attributeValue = Data.split("=", 2)[1];
        String value = "";
        try {
            LocatorAssertions.HasCSSOptions options = new LocatorAssertions.HasCSSOptions();
            options.setTimeout(getTimeoutValue());
            value = (String) Locator.evaluate("(element) => window.getComputetStyle(element).getPropertyValue(" + attributeName + ")");
            highlightElement();
            assertThat(Locator).hasCSS(attributeName, attributeValue, options);
            Report.updateTestLog(Action, "[" + ObjectName + "] has CSS attribute '" + attributeName + "' with value '" + attributeValue + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have CSS attribute '" + attributeName + "' with value '" + attributeValue + "'. Actual value is '" + value + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not have the CSS [<Data>]", input = InputType.YES)
    public void assertElementCSSNotMatches() {
        String attributeName = Data.split("=")[0];
        String attributeValue = Data.split("=")[1];
        String value = "";
        try {
            LocatorAssertions.HasCSSOptions options = new LocatorAssertions.HasCSSOptions();
            options.setTimeout(getTimeoutValue());
            value = (String) Locator.evaluate("(element) => window.getComputetStyle(element).getPropertyValue(" + attributeName + ")");
            highlightElement();
            assertThat(Locator).not().hasCSS(attributeName, attributeValue, options);
            Report.updateTestLog(Action, "[" + ObjectName + "] does not have CSS attribute '" + attributeName + "' with value '" + attributeValue + "'. Actual value is '" + value + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has CSS attribute '" + attributeName + "' with value '" + attributeValue + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'ID' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has ID [<Data>]", input = InputType.YES)
    public void assertElementIdMatches() {
        String actualIdValue = "";
        try {
            LocatorAssertions.HasIdOptions options = new LocatorAssertions.HasIdOptions();
            options.setTimeout(getTimeoutValue());
            actualIdValue = Locator.getAttribute("id");
            highlightElement();
            assertThat(Locator).hasId(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] has 'ID' matching '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have 'ID' matching '" + Data + "'. Actual value is '" + actualIdValue + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not have ID [<Data>]", input = InputType.YES)
    public void assertElementIdNotMatches() {
        String actualIdValue = "";
        try {
            LocatorAssertions.HasIdOptions options = new LocatorAssertions.HasIdOptions();
            options.setTimeout(getTimeoutValue());
            actualIdValue = Locator.getAttribute("id");
            highlightElement();
            assertThat(Locator).not().hasId(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] does not have 'ID' matching '" + Data + "'. Actual value is '" + actualIdValue + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has 'ID' matching '" + Data + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'JS Property' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has JS Property [<Data>]", input = InputType.YES)
    public void assertElementJSPropertyMatches() {
        String attributeName = Data.split("=")[0];
        String attributeValue = Data.split("=")[1];
        try {
            LocatorAssertions.HasJSPropertyOptions options = new LocatorAssertions.HasJSPropertyOptions();
            options.setTimeout(getTimeoutValue());            
            highlightElement();
            assertThat(Locator).hasJSProperty(attributeName, attributeValue, options);
            Report.updateTestLog(Action, "[" + ObjectName + "] has JS Property attribute '" + attributeName + "' with value '" + attributeValue + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have JS Property attribute '" + attributeName + "' with value '" + attributeValue + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not have JS Property [<Data>]", input = InputType.YES)
    public void assertElementJSPropertyNotMatches() {
        String attributeName = Data.split("=")[0];
        String attributeValue = Data.split("=")[1];
        try {
            LocatorAssertions.HasJSPropertyOptions options = new LocatorAssertions.HasJSPropertyOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).not().hasJSProperty(attributeName, attributeValue, options);
            Report.updateTestLog(Action, "[" + ObjectName + "] does not have JS Property attribute '" + attributeName + "' with value '" + attributeValue + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has JS Property attribute '" + attributeName + "' with value '" + attributeValue + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'Role' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has Role [<Data>]", input = InputType.YES)
    public void assertElementRoleMatches() {
        String actualIdValue = "";
        try {
            LocatorAssertions.HasRoleOptions options = new LocatorAssertions.HasRoleOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).hasRole(AriaRole.valueOf(Data.toUpperCase()), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] has 'Role' matching '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have 'Role' matching '" + Data + "'. Actual value is '" + actualIdValue + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not have Role [<Data>]", input = InputType.YES)
    public void assertElementRoleNotMatches() {
        try {
            LocatorAssertions.HasRoleOptions options = new LocatorAssertions.HasRoleOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).not().hasRole(AriaRole.valueOf(Data.toUpperCase()), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] does not have 'Role' matching '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has 'Role' matching '" + Data + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'Text' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has text [<Data>]", input = InputType.YES)
    public void assertElementTextMatches() {
        String text = "";
        try {
            LocatorAssertions.HasTextOptions options = new LocatorAssertions.HasTextOptions();
            options.setTimeout(getTimeoutValue());
            text = Locator.innerHTML();
            highlightElement();
            assertThat(Locator).hasText(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] has text '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have text '" + Data + "'. Actual text is '" + text + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not have text [<Data>]", input = InputType.YES)
    public void assertElementTextNotMatches() {
        String text = "";
        try {
            LocatorAssertions.HasTextOptions options = new LocatorAssertions.HasTextOptions();
            options.setTimeout(getTimeoutValue());
            text = Locator.innerHTML();
            highlightElement();
            assertThat(Locator).not().hasText(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] does not have text '" + Data + "'. Actual text is '" + text + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has text '" + Data + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * * Assertion for 'Value' **
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has value [<Data>]", input = InputType.YES)
    public void assertElementValueMatches() {

        String value = "";
        try {
            LocatorAssertions.HasValueOptions options = new LocatorAssertions.HasValueOptions();
            options.setTimeout(getTimeoutValue());
            value = Locator.getAttribute("value");
            highlightElement();
            assertThat(Locator).hasValue(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] has value '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have value '" + Data + "'. Actual value is '" + value + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not value [<Data>]", input = InputType.YES)
    public void assertElementValueNotMatches() {

        String value = "";
        try {
            LocatorAssertions.HasValueOptions options = new LocatorAssertions.HasValueOptions();
            options.setTimeout(getTimeoutValue());
            value = Locator.getAttribute("value");
            highlightElement();
            assertThat(Locator).not().hasValue(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "[" + ObjectName + "] does not have value '" + Data + "'. Actual value is '" + value + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has value '" + Data + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] has values [<Data>]", input = InputType.YES)
    public void assertElementValuesMatch() {
        try {
            LocatorAssertions.HasValuesOptions options = new LocatorAssertions.HasValuesOptions();
            options.setTimeout(getTimeoutValue());
            String values[] = Data.split("=");
            Pattern[] pattern = new Pattern[values.length];
            for (int i = 0; i < values.length; i++) {
                pattern[i] = Pattern.compile(values[i]);
            }
            highlightElement();
            assertThat(Locator).hasValues(pattern, options);
            Report.updateTestLog(Action, "[" + ObjectName + "] has values '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] does not have values '" + Data + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not have values [<Data>]", input = InputType.YES)
    public void assertElementValuesNotMatch() {
        try {
            LocatorAssertions.HasValuesOptions options = new LocatorAssertions.HasValuesOptions();
            options.setTimeout(getTimeoutValue());
            String values[] = Data.split("=");
            Pattern[] pattern = new Pattern[values.length];
            for (int i = 0; i < values.length; i++) {
                pattern[i] = Pattern.compile(values[i]);
            }
            highlightElement();
            assertThat(Locator).not().hasValues(pattern, options);
            Report.updateTestLog(Action, "[" + ObjectName + "] does not have values '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] has values '" + Data + "'");
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * *************************************************************************************************************
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] points to an attached DOM node")
    public void assertElementIsAttached() {
        try {
            LocatorAssertions.IsAttachedOptions options = new LocatorAssertions.IsAttachedOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isAttached(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is attached to the DOM", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not attached to the DOM");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] does not point to an attached DOM node")
    public void assertElementIsNotAttached() {
        try {
            LocatorAssertions.IsAttachedOptions options = new LocatorAssertions.IsAttachedOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isAttached(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not attached to the DOM", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is attached to the DOM");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is checked")
    public void assertElementIsChecked() {
        try {
            LocatorAssertions.IsCheckedOptions options = new LocatorAssertions.IsCheckedOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isChecked(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is checked", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not checked");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not checked")
    public void assertElementIsNotChecked() {
        try {
            LocatorAssertions.IsCheckedOptions options = new LocatorAssertions.IsCheckedOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isChecked(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not checked", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is checked");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is disabled")
    public void assertElementIsDisabled() {
        try {
            LocatorAssertions.IsDisabledOptions options = new LocatorAssertions.IsDisabledOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isDisabled(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is disabled", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not disabled");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not disabled")
    public void assertElementIsNotDisabled() {
        try {
            LocatorAssertions.IsDisabledOptions options = new LocatorAssertions.IsDisabledOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isDisabled(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not disabled", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is disabled");
        } 
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is editable")
    public void assertElementIsEditable() {
        try {
            LocatorAssertions.IsEditableOptions options = new LocatorAssertions.IsEditableOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isEditable(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is editable", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not editable");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not editable")
    public void assertElementIsNotEditable() {
        try {
            LocatorAssertions.IsEditableOptions options = new LocatorAssertions.IsEditableOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isEditable(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not editable", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is editable");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is empty")
    public void assertElementIsEmpty() {
        try {
            LocatorAssertions.IsEmptyOptions options = new LocatorAssertions.IsEmptyOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isEmpty(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is empty", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not empty");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not empty")
    public void assertElementIsNotEmpty() {
        try {
            LocatorAssertions.IsEmptyOptions options = new LocatorAssertions.IsEmptyOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isEmpty(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not empty", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is empty");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is enabled")
    public void assertElementIsEnabled() {
        try {
            LocatorAssertions.IsEnabledOptions options = new LocatorAssertions.IsEnabledOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isEnabled(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is enabled", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not enabled");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not enabled")
    public void assertElementIsNotEnabled() {
        try {
            LocatorAssertions.IsEnabledOptions options = new LocatorAssertions.IsEnabledOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isEnabled(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not enabled", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is enabled");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is focused")
    public void assertElementIsFocused() {
        try {
            LocatorAssertions.IsFocusedOptions options = new LocatorAssertions.IsFocusedOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isFocused(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is focused", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not focused");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not focused")
    public void assertElementIsNotFocused() {
        try {
            LocatorAssertions.IsFocusedOptions options = new LocatorAssertions.IsFocusedOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isFocused(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not focused", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is focused");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is hidden")
    public void assertElementIsHidden() {
        try {
            LocatorAssertions.IsHiddenOptions options = new LocatorAssertions.IsHiddenOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).isHidden(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is hidden", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not hidden");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not hidden")
    public void assertElementIsNotHidden() {
        try {
            LocatorAssertions.IsHiddenOptions options = new LocatorAssertions.IsHiddenOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isHidden(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not hidden", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is hidden");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is in viewport")
    public void assertElementIsInViewport() {
        try {
            LocatorAssertions.IsInViewportOptions options = new LocatorAssertions.IsInViewportOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isInViewport(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is in viewport", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not in viewport");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not in viewport")
    public void assertElementIsNotInViewport() {
        try {
            LocatorAssertions.IsInViewportOptions options = new LocatorAssertions.IsInViewportOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isInViewport(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not in viewport", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is in viewport");
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is visible")
    public void assertElementIsVisible() {
        try {
            LocatorAssertions.IsVisibleOptions options = new LocatorAssertions.IsVisibleOptions();
            options.setTimeout(getTimeoutValue());
            highlightElement();
            assertThat(Locator).isVisible(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is visible", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is not visible");
        } finally {
            removeHighlightFromElement();
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] is not visible")
    public void assertElementIsNotVisible() {
        try {
            LocatorAssertions.IsVisibleOptions options = new LocatorAssertions.IsVisibleOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Locator).not().isVisible(options);
            Report.updateTestLog(Action, "[" + ObjectName + "] is not visible", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, "[" + ObjectName + "] is visible");
        }
    }

    /**
     * *************************************************************************************************************
     */
    @Action(object = ObjectType.BROWSER, desc = "Assert if Page has title [<Data>]", input = InputType.YES)
    public void assertPageTitleMatches() {

        try {
            PageAssertions.HasTitleOptions options = new PageAssertions.HasTitleOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Page).hasTitle(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "Page has title matching '" + Data + "'", Status.PASS);
        } catch (AssertionFailedError e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Assertion Failed", "Page does not have title matching '" + Data + "'", Status.FAIL);
        } catch (PlaywrightException e) {
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Assert if Page has URL [<Data>]", input = InputType.YES)
    public void assertPageURLMatches() {

        try {
            PageAssertions.HasURLOptions options = new PageAssertions.HasURLOptions();
            options.setTimeout(getTimeoutValue());
            assertThat(Page).hasURL(Pattern.compile(Data), options);
            Report.updateTestLog(Action, "Page has URL matching '" + Data + "'", Status.PASS);
        } catch (AssertionFailedError e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Assertion Failed", "Page does not have URL matching '" + Data + "'", Status.FAIL);
        } catch (PlaywrightException e) {
            throw new ActionException(e);
        }
    }

    private void PlaywrightExceptionLogging(PlaywrightException e) {
        Report.updateTestLog(Action, "Unique Element [" + ObjectName + "] not found on Page. Error :" + e.getMessage(), Status.FAIL);
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
        throw new ActionException(e);
    }

    private void assertionLogging(AssertionFailedError err, String message) {
        if (err.getMessage().contains("locator resolved to")) {
            Report.updateTestLog(Action, message, Status.FAIL);
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found on Page \n", Status.FAIL);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, err);
    }

    /**
     * *********************************************************************************************
     */
    private void highlightElement() {
        Locator.scrollIntoViewIfNeeded();
        Locator.evaluate("element => element.style.outline = '2px solid red'");
    }

    private void removeHighlightFromElement() {
        Locator.evaluate("element => element.style.outline = ''");
    }

    private double getTimeoutValue() {
        double timeout = 5000;
        if (StringUtils.isNotBlank(Condition)) {
            try {
                timeout = Double.parseDouble(Condition.trim());
            } catch (NumberFormatException e) {
                Report.updateTestLog(Action, "'" + Condition + "' cannot be converted to timeout of type Double", Status.DEBUG);
            }
        }
        return timeout;
    }
}
package com.ing.datalib.or.yaml;

import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobilePlatform;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML representation of a Mobile OR element.
 * <p>
 * Each element exposes two nested platform blocks – {@code android} and
 * {@code ios} – each with its own locator strategies. The execution engine
 * picks the appropriate block at runtime based on the Appium driver.
 * <p>
 * Example YAML output:
 * <pre>
 * loginButton:
 *   android:
 *     uiAutomator: new UiSelector().text("Login")
 *     id: com.example.app:id/login
 *   ios:
 *     uiAutomation: ...
 *     accessibility: login_button
 * </pre>
 * <p>
 * Repositories produced by earlier INGenious versions use a flat layout with
 * locator keys at the top of the element. Those files continue to load – the
 * top-level fields are migrated into both platform blocks on read.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"android", "ios"})
public class YamlMobileElementDefinition {

    private PlatformLocators android;
    private PlatformLocators ios;

    // ---- Legacy flat fields, retained for backward-compat read only. ----
    @JsonIgnore private String uiAutomator;
    @JsonIgnore private String uiAutomation;
    @JsonIgnore private String id;
    @JsonIgnore private String accessibility;
    @JsonIgnore private String xpath;
    @JsonIgnore private String css;
    @JsonIgnore private String name;
    @JsonIgnore private String tagName;
    @JsonIgnore private String linkText;
    @JsonIgnore private String className;
    @JsonIgnore private List<String> exact;

    public YamlMobileElementDefinition() {
    }

    // =================== Nested platform blocks ===================

    public PlatformLocators getAndroid() {
        return android;
    }

    public void setAndroid(PlatformLocators android) {
        this.android = android;
    }

    public PlatformLocators getIos() {
        return ios;
    }

    public void setIos(PlatformLocators ios) {
        this.ios = ios;
    }

    // =================== Legacy setters (read-only) ===================
    // Jackson invokes these when the YAML file uses the flat layout from
    // earlier INGenious releases. They populate the corresponding nested
    // platform block.

    @JsonSetter("uiAutomator")
    public void setUiAutomator(String v) { ensureAndroid().setUiAutomator(v); }

    @JsonSetter("uiAutomation")
    public void setUiAutomation(String v) { ensureIos().setUiAutomation(v); }

    @JsonSetter("id")
    public void setId(String v) {
        ensureAndroid().setId(v);
        ensureIos().setId(v);
    }

    @JsonSetter("accessibility")
    public void setAccessibility(String v) {
        ensureAndroid().setAccessibility(v);
        ensureIos().setAccessibility(v);
    }

    @JsonSetter("xpath")
    public void setXpath(String v) {
        ensureAndroid().setXpath(v);
        ensureIos().setXpath(v);
    }

    @JsonSetter("css")
    public void setCss(String v) {
        ensureAndroid().setCss(v);
        ensureIos().setCss(v);
    }

    @JsonSetter("name")
    public void setName(String v) {
        ensureAndroid().setName(v);
        ensureIos().setName(v);
    }

    @JsonSetter("tagName")
    public void setTagName(String v) {
        ensureAndroid().setTagName(v);
        ensureIos().setTagName(v);
    }

    @JsonSetter("linkText")
    public void setLinkText(String v) {
        ensureAndroid().setLinkText(v);
        ensureIos().setLinkText(v);
    }

    @JsonSetter("className")
    public void setClassName(String v) {
        ensureAndroid().setClassName(v);
        ensureIos().setClassName(v);
    }

    @JsonSetter("exact")
    public void setExact(List<String> exact) {
        if (exact == null || exact.isEmpty()) {
            return;
        }
        ensureAndroid().setExact(new ArrayList<>(exact));
        ensureIos().setExact(new ArrayList<>(exact));
    }

    private PlatformLocators ensureAndroid() {
        if (android == null) {
            android = new PlatformLocators();
        }
        return android;
    }

    private PlatformLocators ensureIos() {
        if (ios == null) {
            ios = new PlatformLocators();
        }
        return ios;
    }

    // =================== Conversion to/from MobileORObject ===================

    /**
     * Convert a MobileORObject to YamlMobileElementDefinition.
     */
    public static YamlMobileElementDefinition fromMobileORObject(MobileORObject obj) {
        YamlMobileElementDefinition yaml = new YamlMobileElementDefinition();
        yaml.android = PlatformLocators.fromAttributes(obj.getAttributes(MobilePlatform.ANDROID));
        yaml.ios = PlatformLocators.fromAttributes(obj.getAttributes(MobilePlatform.IOS));
        if (yaml.android != null && yaml.android.isEmpty()) {
            yaml.android = null;
        }
        if (yaml.ios != null && yaml.ios.isEmpty()) {
            yaml.ios = null;
        }
        return yaml;
    }

    /**
     * Convert YamlMobileElementDefinition to a MobileORObject.
     */
    public MobileORObject toMobileORObject(String name, ObjectGroup<MobileORObject> group) {
        MobileORObject obj = new MobileORObject(name, group);
        if (android != null) {
            android.applyTo(obj, MobilePlatform.ANDROID);
        }
        if (ios != null) {
            ios.applyTo(obj, MobilePlatform.IOS);
        }
        return obj;
    }

    /**
     * Check if this element has any defined locators on either platform.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return (android == null || android.isEmpty())
            && (ios == null || ios.isEmpty());
    }

    /**
     * Get the primary locator value (first non-empty locator) preferring
     * the Android block (which has historically been the default).
     */
    @JsonIgnore
    public String getPrimaryLocatorValue() {
        String v = android != null ? android.primaryValue() : null;
        if (v == null && ios != null) {
            v = ios.primaryValue();
        }
        return v;
    }

    @JsonIgnore
    public String getPrimaryLocatorType() {
        String v = android != null ? android.primaryType() : null;
        if (v == null && ios != null) {
            v = ios.primaryType();
        }
        return v;
    }

    // =================================================================
    // Per-platform locator block
    // =================================================================
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder({"uiAutomator", "uiAutomation", "id", "accessibility",
                        "xpath", "css", "name", "tagName", "linkText", "className", "exact"})
    public static class PlatformLocators {

        private String uiAutomator;
        private String uiAutomation;
        private String id;
        private String accessibility;
        private String xpath;
        private String css;
        private String name;
        private String tagName;
        @JsonProperty("linkText")
        private String linkText;
        @JsonProperty("className")
        private String className;
        private List<String> exact;

        public PlatformLocators() {
        }

        public String getUiAutomator() { return uiAutomator; }
        public void setUiAutomator(String v) { this.uiAutomator = v; }

        public String getUiAutomation() { return uiAutomation; }
        public void setUiAutomation(String v) { this.uiAutomation = v; }

        public String getId() { return id; }
        public void setId(String v) { this.id = v; }

        public String getAccessibility() { return accessibility; }
        public void setAccessibility(String v) { this.accessibility = v; }

        public String getXpath() { return xpath; }
        public void setXpath(String v) { this.xpath = v; }

        public String getCss() { return css; }
        public void setCss(String v) { this.css = v; }

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }

        public String getTagName() { return tagName; }
        public void setTagName(String v) { this.tagName = v; }

        public String getLinkText() { return linkText; }
        public void setLinkText(String v) { this.linkText = v; }

        public String getClassName() { return className; }
        public void setClassName(String v) { this.className = v; }

        public List<String> getExact() { return exact; }
        public void setExact(List<String> exact) { this.exact = exact; }

        public void addExact(String name) {
            if (this.exact == null) {
                this.exact = new ArrayList<>();
            }
            if (!this.exact.contains(name.toLowerCase())) {
                this.exact.add(name.toLowerCase());
            }
        }

        public boolean isExact(String name) {
            return exact != null && exact.contains(name.toLowerCase());
        }

        @JsonIgnore
        public boolean isEmpty() {
            return isNullOrEmpty(uiAutomator) && isNullOrEmpty(uiAutomation)
                && isNullOrEmpty(id) && isNullOrEmpty(accessibility)
                && isNullOrEmpty(xpath) && isNullOrEmpty(css)
                && isNullOrEmpty(name) && isNullOrEmpty(tagName)
                && isNullOrEmpty(linkText) && isNullOrEmpty(className);
        }

        private static boolean isNullOrEmpty(String s) {
            return s == null || s.isEmpty();
        }

        static PlatformLocators fromAttributes(List<ORAttribute> attrs) {
            PlatformLocators p = new PlatformLocators();
            if (attrs == null) {
                return p;
            }
            for (ORAttribute attr : attrs) {
                String value = attr.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                switch (attr.getName()) {
                    case "UiAutomator":   p.uiAutomator = value; break;
                    case "UiAutomation":  p.uiAutomation = value; break;
                    case "id":            p.id = value; break;
                    case "Accessibility": p.accessibility = value; break;
                    case "xpath":         p.xpath = value; break;
                    case "css":           p.css = value; break;
                    case "name":          p.name = value; break;
                    case "tagName":       p.tagName = value; break;
                    case "link_text":     p.linkText = value; break;
                    case "class":         p.className = value; break;
                    default: break; // ignore unknown
                }
                if (attr.isExact()) {
                    p.addExact(attr.getName());
                }
            }
            return p;
        }

        void applyTo(MobileORObject obj, MobilePlatform platform) {
            setAttr(obj, platform, "UiAutomator",   uiAutomator,   isExact("uiautomator"));
            setAttr(obj, platform, "UiAutomation",  uiAutomation,  isExact("uiautomation"));
            setAttr(obj, platform, "id",            id,            isExact("id"));
            setAttr(obj, platform, "Accessibility", accessibility, isExact("accessibility"));
            setAttr(obj, platform, "xpath",         xpath,         isExact("xpath"));
            setAttr(obj, platform, "css",           css,           isExact("css"));
            setAttr(obj, platform, "name",          name,          isExact("name"));
            setAttr(obj, platform, "tagName",       tagName,       isExact("tagname"));
            setAttr(obj, platform, "link_text",     linkText,      isExact("link_text"));
            setAttr(obj, platform, "class",         className,     isExact("class"));
        }

        private void setAttr(MobileORObject obj, MobilePlatform platform,
                             String propName, String value, boolean exactMatch) {
            ORAttribute attr = obj.getAttribute(platform, propName);
            if (attr != null && value != null && !value.isEmpty()) {
                attr.setValue(value);
                attr.setExact(exactMatch);
            }
        }

        @JsonIgnore
        String primaryValue() {
            if (!isNullOrEmpty(accessibility)) return accessibility;
            if (!isNullOrEmpty(id)) return id;
            if (!isNullOrEmpty(uiAutomator)) return uiAutomator;
            if (!isNullOrEmpty(uiAutomation)) return uiAutomation;
            if (!isNullOrEmpty(xpath)) return xpath;
            if (!isNullOrEmpty(css)) return css;
            if (!isNullOrEmpty(name)) return name;
            if (!isNullOrEmpty(tagName)) return tagName;
            if (!isNullOrEmpty(linkText)) return linkText;
            if (!isNullOrEmpty(className)) return className;
            return null;
        }

        @JsonIgnore
        String primaryType() {
            if (!isNullOrEmpty(accessibility)) return "Accessibility";
            if (!isNullOrEmpty(id)) return "id";
            if (!isNullOrEmpty(uiAutomator)) return "UiAutomator";
            if (!isNullOrEmpty(uiAutomation)) return "UiAutomation";
            if (!isNullOrEmpty(xpath)) return "xpath";
            if (!isNullOrEmpty(css)) return "css";
            if (!isNullOrEmpty(name)) return "name";
            if (!isNullOrEmpty(tagName)) return "tagName";
            if (!isNullOrEmpty(linkText)) return "link_text";
            if (!isNullOrEmpty(className)) return "class";
            return null;
        }
    }
}

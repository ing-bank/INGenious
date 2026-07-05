package com.ing.datalib.or.yaml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobilePlatform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
@JsonPropertyOrder({ "android", "ios" })
public class YamlMobileElementDefinition {
    private PlatformLocators android;
    private PlatformLocators ios;

    // ---- Legacy flat fields, retained for backward-compat read only. ----
    @JsonIgnore
    private String uiAutomator;

    @JsonIgnore
    private String uiAutomation;

    @JsonIgnore
    private String id;

    @JsonIgnore
    private String accessibility;

    @JsonIgnore
    private String xpath;

    @JsonIgnore
    private String css;

    @JsonIgnore
    private String name;

    @JsonIgnore
    private String tagName;

    @JsonIgnore
    private String linkText;

    @JsonIgnore
    private String className;

    @JsonIgnore
    private List<String> exact;

    public YamlMobileElementDefinition() {}

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
    public void setUiAutomator(String v) {
        ensureAndroid().setUiAutomator(v);
    }

    @JsonSetter("uiAutomation")
    public void setUiAutomation(String v) {
        ensureIos().setUiAutomation(v);
    }

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
        return (android == null || android.isEmpty()) && (ios == null || ios.isEmpty());
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder(
        {
            "uiAutomator",
            "uiAutomation",
            "id",
            "accessibility",
            "xpath",
            "css",
            "name",
            "tagName",
            "linkText",
            "className",
            "exact"
        }
    )
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

        /** Captures custom (user-defined) attribute name-value pairs not in the known locator set. */
        private Map<String, String> additionalProperties = new LinkedHashMap<>();

        public PlatformLocators() {}

        @JsonAnyGetter
        public Map<String, String> getAdditionalProperties() {
            return additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String key, String value) {
            additionalProperties.put(key, value);
        }

        public String getUiAutomator() {
            return isNullOrEmpty(uiAutomator) ? null : uiAutomator;
        }

        public void setUiAutomator(String v) {
            this.uiAutomator = v;
        }

        public String getUiAutomation() {
            return isNullOrEmpty(uiAutomation) ? null : uiAutomation;
        }

        public void setUiAutomation(String v) {
            this.uiAutomation = v;
        }

        public String getId() {
            return isNullOrEmpty(id) ? null : id;
        }

        public void setId(String v) {
            this.id = v;
        }

        public String getAccessibility() {
            return isNullOrEmpty(accessibility) ? null : accessibility;
        }

        public void setAccessibility(String v) {
            this.accessibility = v;
        }

        public String getXpath() {
            return isNullOrEmpty(xpath) ? null : xpath;
        }

        public void setXpath(String v) {
            this.xpath = v;
        }

        public String getCss() {
            return isNullOrEmpty(css) ? null : css;
        }

        public void setCss(String v) {
            this.css = v;
        }

        public String getName() {
            return isNullOrEmpty(name) ? null : name;
        }

        public void setName(String v) {
            this.name = v;
        }

        public String getTagName() {
            return isNullOrEmpty(tagName) ? null : tagName;
        }

        public void setTagName(String v) {
            this.tagName = v;
        }

        public String getLinkText() {
            return isNullOrEmpty(linkText) ? null : linkText;
        }

        public void setLinkText(String v) {
            this.linkText = v;
        }

        public String getClassName() {
            return isNullOrEmpty(className) ? null : className;
        }

        public void setClassName(String v) {
            this.className = v;
        }

        public List<String> getExact() {
            return exact;
        }

        public void setExact(List<String> exact) {
            this.exact = exact;
        }

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
            return (
                isNullOrEmpty(uiAutomator) &&
                isNullOrEmpty(uiAutomation) &&
                isNullOrEmpty(id) &&
                isNullOrEmpty(accessibility) &&
                isNullOrEmpty(xpath) &&
                isNullOrEmpty(css) &&
                isNullOrEmpty(name) &&
                isNullOrEmpty(tagName) &&
                isNullOrEmpty(linkText) &&
                isNullOrEmpty(className) &&
                (additionalProperties == null || additionalProperties.isEmpty())
            );
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
                String attrName = attr.getName();

                switch (attrName) {
                    case "UiAutomator":
                        if (value != null && !value.isEmpty()) {
                            p.uiAutomator = value;
                        }
                        break;
                    case "UiAutomation":
                        if (value != null && !value.isEmpty()) {
                            p.uiAutomation = value;
                        }
                        break;
                    case "id":
                        if (value != null && !value.isEmpty()) {
                            p.id = value;
                        }
                        break;
                    case "Accessibility":
                        if (value != null && !value.isEmpty()) {
                            p.accessibility = value;
                        }
                        break;
                    case "xpath":
                        if (value != null && !value.isEmpty()) {
                            p.xpath = value;
                        }
                        break;
                    case "css":
                        if (value != null && !value.isEmpty()) {
                            p.css = value;
                        }
                        break;
                    case "name":
                        if (value != null && !value.isEmpty()) {
                            p.name = value;
                        }
                        break;
                    case "tagName":
                        if (value != null && !value.isEmpty()) {
                            p.tagName = value;
                        }
                        break;
                    case "link_text":
                        if (value != null && !value.isEmpty()) {
                            p.linkText = value;
                        }
                        break;
                    case "class":
                        if (value != null && !value.isEmpty()) {
                            p.className = value;
                        }
                        break;
                    default:
                        // Custom (user-defined) attributes — capture even when empty
                        p.additionalProperties.put(attrName, value != null ? value : "");
                        break;
                }
                if (attr.isExact()) {
                    p.addExact(attrName);
                }
            }
            return p;
        }

        void applyTo(MobileORObject obj, MobilePlatform platform) {
            setAttr(obj, platform, "UiAutomator", uiAutomator, isExact("uiautomator"));
            setAttr(obj, platform, "UiAutomation", uiAutomation, isExact("uiautomation"));
            setAttr(obj, platform, "id", id, isExact("id"));
            setAttr(obj, platform, "Accessibility", accessibility, isExact("accessibility"));
            setAttr(obj, platform, "xpath", xpath, isExact("xpath"));
            setAttr(obj, platform, "css", css, isExact("css"));
            setAttr(obj, platform, "name", name, isExact("name"));
            setAttr(obj, platform, "tagName", tagName, isExact("tagname"));
            setAttr(obj, platform, "link_text", linkText, isExact("link_text"));
            setAttr(obj, platform, "class", className, isExact("class"));
            // Restore custom (user-defined) properties
            if (additionalProperties != null) {
                for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
                    obj.addNewAttribute(platform, entry.getKey());
                    ORAttribute attr = obj.getAttribute(platform, entry.getKey());
                    if (attr != null && entry.getValue() != null) {
                        attr.setValue(entry.getValue());
                    }
                }
            }
        }

        private void setAttr(
            MobileORObject obj,
            MobilePlatform platform,
            String propName,
            String value,
            boolean exactMatch
        ) {
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

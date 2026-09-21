package com.ing.ide.main.mobilerecorder;

import com.ing.datalib.or.mobile.MobilePlatform;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Resolves a tap point (in device logical coordinates) to the most specific UI
 * element under it, using the current page-source XML, and derives a stable
 * locator for it.
 *
 * <p>Selection strategy: of all elements whose bounds contain the point, pick the
 * one with the smallest bounding-box area (the most deeply nested / most specific
 * match), mirroring how a human identifies "the button I tapped" rather than one
 * of its ancestor containers.
 *
 * <p>The resulting {@link ResolvedElement#strategy} is one of the INGenious Mobile
 * OR locator property names ({@code id}, {@code Accessibility}, {@code class},
 * {@code xpath}) so it maps directly onto a {@code MobileORObject} attribute.
 */
public final class MobileLocatorResolver {
    private static final Logger LOG = Logger.getLogger(MobileLocatorResolver.class.getName());

    // Android bounds="[left,top][right,bottom]"
    private static final Pattern ANDROID_BOUNDS = Pattern.compile(
        "\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]"
    );

    public ResolvedElement resolve(String pageSourceXml, MobilePlatform platform, int x, int y) {
        Element best = null;
        long bestArea = Long.MAX_VALUE;
        try {
            Document doc = parse(pageSourceXml);
            NodeList all = doc.getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                Element el = (Element) all.item(i);
                Rect bounds = readBounds(el, platform);
                if (bounds == null || !bounds.contains(x, y)) {
                    continue;
                }
                long area = bounds.area();
                if (area < bestArea) {
                    bestArea = area;
                    best = el;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse page source", e);
            return null;
        }
        if (best == null) {
            return null;
        }
        return buildResolved(best, platform);
    }

    private ResolvedElement buildResolved(Element el, MobilePlatform platform) {
        String strategy;
        String value;
        if (platform == MobilePlatform.IOS) {
            String name = el.getAttribute("name");
            String label = el.getAttribute("label");
            String type = el.getTagName();
            if (notBlank(name)) {
                strategy = "Accessibility";
                value = name;
            } else if (notBlank(label)) {
                strategy = "xpath";
                value = "//" + type + "[@label=\"" + label + "\"]";
            } else {
                strategy = "xpath";
                value = "//" + type;
            }
        } else {
            String resourceId = el.getAttribute("resource-id");
            String contentDesc = el.getAttribute("content-desc");
            String cls = el.getTagName();
            String text = el.getAttribute("text");
            if (notBlank(resourceId)) {
                strategy = "id";
                value = resourceId;
            } else if (notBlank(contentDesc)) {
                strategy = "Accessibility";
                value = contentDesc;
            } else if (notBlank(text)) {
                strategy = "xpath";
                value = "//" + cls + "[@text=\"" + text + "\"]";
            } else {
                strategy = "xpath";
                value = "//" + cls;
            }
        }
        return new ResolvedElement(
            strategy,
            value,
            el.getTagName(),
            describe(el, platform),
            isEditable(el, platform)
        );
    }

    private boolean isEditable(Element el, MobilePlatform platform) {
        String tag = el.getTagName().toLowerCase();
        if (platform == MobilePlatform.IOS) {
            return (
                tag.equals("xcuielementtypetextfield") ||
                tag.equals("xcuielementtypesecuretextfield") ||
                tag.equals("xcuielementtypetextview") ||
                tag.equals("xcuielementtypesearchfield")
            );
        }
        return tag.contains("edittext") || tag.contains("autocompletetextview");
    }

    private String describe(Element el, MobilePlatform platform) {
        String label = platform == MobilePlatform.IOS
            ? firstNonBlank(el.getAttribute("label"), el.getAttribute("name"))
            : firstNonBlank(el.getAttribute("text"), el.getAttribute("content-desc"));
        String simpleTag = simpleTagName(el.getTagName());
        return notBlank(label) ? simpleTag + " \"" + label + "\"" : simpleTag;
    }

    private static String simpleTagName(String tag) {
        int dot = tag.lastIndexOf('.');
        String base = dot >= 0 ? tag.substring(dot + 1) : tag;
        return base.replace("XCUIElementType", "");
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private Rect readBounds(Element el, MobilePlatform platform) {
        if (platform == MobilePlatform.IOS) {
            String xs = el.getAttribute("x");
            String ys = el.getAttribute("y");
            String ws = el.getAttribute("width");
            String hs = el.getAttribute("height");
            if (isBlank(xs) || isBlank(ys) || isBlank(ws) || isBlank(hs)) {
                return null;
            }
            try {
                double left = Double.parseDouble(xs);
                double top = Double.parseDouble(ys);
                double width = Double.parseDouble(ws);
                double height = Double.parseDouble(hs);
                return new Rect((int) left, (int) top, (int) (left + width), (int) (top + height));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        String bounds = el.getAttribute("bounds");
        if (isBlank(bounds)) {
            return null;
        }
        Matcher m = ANDROID_BOUNDS.matcher(bounds);
        if (!m.matches()) {
            return null;
        }
        int left = Integer.parseInt(m.group(1));
        int top = Integer.parseInt(m.group(2));
        int right = Integer.parseInt(m.group(3));
        int bottom = Integer.parseInt(m.group(4));
        return new Rect(left, top, right, bottom);
    }

    private static String firstNonBlank(String a, String b) {
        return isBlank(a) ? b : a;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean notBlank(String s) {
        return !isBlank(s);
    }

    /**
     * A resolved locator plus a short human-readable description of the tapped
     * element, and whether it accepts typed text input.
     */
    public static final class ResolvedElement {
        private final String strategy;
        private final String value;
        private final String className;
        private final String description;
        private final boolean editable;

        public ResolvedElement(
            String strategy,
            String value,
            String className,
            String description,
            boolean editable
        ) {
            this.strategy = strategy;
            this.value = value;
            this.className = className;
            this.description = description;
            this.editable = editable;
        }

        public String getStrategy() {
            return strategy;
        }

        public String getValue() {
            return value;
        }

        public String getClassName() {
            return className;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEditable() {
            return editable;
        }
    }

    private static final class Rect {
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        Rect(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        boolean contains(int x, int y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }

        long area() {
            return (long) Math.max(0, right - left) * Math.max(0, bottom - top);
        }
    }
}

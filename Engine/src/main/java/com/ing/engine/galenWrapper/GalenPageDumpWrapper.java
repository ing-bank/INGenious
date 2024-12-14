
package com.ing.engine.galenWrapper;

import com.ing.engine.constants.FilePath;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galenframework.api.GalenPageDump;
import com.galenframework.api.PageDump;
import com.galenframework.page.PageElement;
import com.galenframework.page.Rect;
import com.galenframework.utils.GalenUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

/**
 *
 * 
 */
public class GalenPageDumpWrapper extends GalenPageDump {

    public GalenPageDumpWrapper(String pageName) {
        super(pageName);
    }

    public void dumpPage(PageValidationWrapper pageValidation, String testCaseName, File reportFolder) throws IOException {
        if (!reportFolder.exists()) {
            if (!reportFolder.mkdirs()) {
                throw new RuntimeException("Cannot create dir: " + reportFolder.getAbsolutePath());
            }
        }

        Set<String> objectNames = pageValidation.elementMap.keySet();

        PageDump pageDump = new PageDump();
        pageDump.setTitle(testCaseName);
        List< Pattern> patterns = convertPatterns(getExcludedObjects());

        for (String objectName : objectNames) {
            if (!matchesExcludedPatterns(objectName, patterns)) {
                PageElement pageElement = pageValidation.findPageElement(objectName);

                if (pageElement.isVisible() && pageElement.getArea() != null) {
                    PageDump.Element element = new PageDump.Element(objectName, pageElement.getArea().toIntArray());

                    if (pageElement.isPresent() && pageElement.isVisible() && isWithinArea(pageElement, getMaxWidth(), getMaxHeight())) {
                        element.setHasImage(true);
                    }
                    pageDump.addElement(element);
                }
            }
        }

        if (!isOnlyImages()) {
            pageDump.setPageName(getPageName());
            exportAsJson(pageDump, new File(reportFolder.getAbsoluteFile() + File.separator + "page.js"));
        }

        exportAllScreenshots(pageDump, pageValidation.getBrowser(), reportFolder);
    }

    @Override
    public void exportAsJson(PageDump dump, File file) throws IOException {
        updatePageMap(dump.getTitle());
        makeSureFileExists(file);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(dump);
        json = "var page=" + json;
        FileUtils.writeStringToFile(file, json, Charset.defaultCharset());
    }

    private boolean matchesExcludedPatterns(String objectName, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(objectName).matches()) {
                return true;
            }
        }
        return false;
    }

    private List<Pattern> convertPatterns(List<String> excludedObjects) {
        List<Pattern> patterns = new LinkedList<>();
        if (excludedObjects != null) {
            for (String excludedObject : excludedObjects) {
                patterns.add(GalenUtils.convertObjectNameRegex(excludedObject));
            }
        }
        return patterns;
    }

    private static boolean isWithinArea(PageElement element, Integer maxWidth, Integer maxHeight) {
        Rect area = element.getArea();
        if (maxWidth != null && maxHeight != null) {
            return maxWidth * maxHeight > area.getWidth() * area.getHeight();
        } else if (maxWidth != null) {
            return maxWidth > area.getWidth();
        } else if (maxHeight != null) {
            return maxHeight > area.getHeight();
        } else {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private void updatePageMap(String pageSource) {
        File file = new File(FilePath.getORpageListJsonFile());
        ObjectMapper objectMapper = new ObjectMapper();
        String varaible = "var pageMap=";
        Map<String, ArrayList<String>> pageMap = new HashMap<>();
        try {
            if (file.exists()) {
                String value = FileUtils.readFileToString(file, Charset.defaultCharset());
                value = value.replace(varaible, "");
                pageMap = objectMapper.readValue(value, Map.class);
            } else {
                file.createNewFile();
            }
            if (pageMap.isEmpty() || !pageMap.containsKey(pageSource)) {
                pageMap.put(pageSource, getPageList(new ArrayList<>()));
            } else {
                pageMap.put(pageSource, getPageList(pageMap.get(pageSource)));
            }
            String jsonVal = objectMapper.writeValueAsString(pageMap);
            FileUtils.writeStringToFile(file, varaible + jsonVal, Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.getLogger(PageDump.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private ArrayList<String> getPageList(ArrayList<String> pageList) {
        if (pageList.isEmpty() || !pageList.contains(getPageName())) {
            pageList.add(getPageName());
        }
        return pageList;
    }
}

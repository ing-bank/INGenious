
package com.ing.engine.galenWrapper.SpecValidation;

import com.ing.engine.commands.galenCommands.General;
import com.ing.engine.constants.FilePath;
import com.ing.engine.galenWrapper.Parser;
import com.galenframework.config.GalenConfig;
import com.galenframework.page.Rect;
import com.galenframework.parser.ExpectNumber;
import com.galenframework.parser.ExpectWord;
import com.galenframework.parser.Expectations;
import com.galenframework.parser.StringCharReader;
import com.galenframework.parser.SyntaxException;
import com.galenframework.rainbow4j.filters.BlurFilter;
import com.galenframework.rainbow4j.filters.ContrastFilter;
import com.galenframework.rainbow4j.filters.DenoiseFilter;
import com.galenframework.rainbow4j.filters.ImageFilter;
import com.galenframework.rainbow4j.filters.QuantinizeFilter;
import com.galenframework.rainbow4j.filters.SaturationFilter;
import com.galenframework.specs.Alignment;
import static com.galenframework.specs.Alignment.ALL;
import static com.galenframework.specs.Alignment.BOTTOM;
import static com.galenframework.specs.Alignment.CENTERED;
import static com.galenframework.specs.Alignment.LEFT;
import static com.galenframework.specs.Alignment.RIGHT;
import static com.galenframework.specs.Alignment.TOP;
import com.galenframework.specs.Location;
import com.galenframework.specs.Range;
import com.galenframework.specs.Side;
import com.galenframework.specs.Spec;
import com.galenframework.specs.SpecAbove;
import com.galenframework.specs.SpecBelow;
import com.galenframework.specs.SpecCentered;
import com.galenframework.specs.SpecColorScheme;
import com.galenframework.specs.SpecContains;
import com.galenframework.specs.SpecCss;
import com.galenframework.specs.SpecHeight;
import com.galenframework.specs.SpecHorizontally;
import com.galenframework.specs.SpecImage;
import com.galenframework.specs.SpecInside;
import com.galenframework.specs.SpecLeftOf;
import com.galenframework.specs.SpecNear;
import com.galenframework.specs.SpecOn;
import com.galenframework.specs.SpecRightOf;
import com.galenframework.specs.SpecText;
import com.galenframework.specs.SpecText.Type;
import com.galenframework.specs.SpecVertically;
import com.galenframework.specs.SpecWidth;
import com.galenframework.specs.colors.ColorRange;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 *
 */
public class SpecReader {

    private static SpecReader specReader;

    public static SpecReader reader() {
        if (specReader == null) {
            specReader = new SpecReader();
        }
        return specReader;
    }

    public SpecContains getSpecContains(List<String> objects, Boolean isPartly) {
        return new SpecContains(objects, isPartly);
    }

    public SpecWidth getSpecWidth(General.RelativeElement rElement, String value, String relativeObjectName) {
        return new SpecWidth(getRange(rElement, value, relativeObjectName, "/width"));
    }

    public SpecHeight getSpecHeight(General.RelativeElement rElement, String value, String relativeObjectName) {
        return new SpecHeight(getRange(rElement, value, relativeObjectName, "/height"));
    }

    private Range getRange(General.RelativeElement rElement, String value, String relativeObjectName, String type) {
        switch (rElement) {
            case None:
                return Parser.parseRange(value);
            case WebElement:
                return Parser.parseRangePercent(value).withPercentOf(relativeObjectName + type);
            default:
                break;
        }
        return null;
    }

    public SpecText getSpecText(Type type, String value) {
        return new SpecText(type, value);
    }

    public SpecCss getSpecCSS(Type type, String value) {
        String cssPropertyName = Expectations.word().read(new StringCharReader(value));
        if (cssPropertyName.isEmpty()) {
            throw new SyntaxException("Expected two values {property (space) value} but only got " + value);
        }
        String cssValue = value.replaceFirst(cssPropertyName + "(,|=|:| )", "");
        return new SpecCss(cssPropertyName, type, cssValue);
    }

    public SpecTitle getSpecTitle(Type type, String value) {
        return new SpecTitle(type, value);
    }

    public SpecUrl getSpecUrl(Type type, String value) {
        return new SpecUrl(type, value);
    }

    public SpecAttribute getSpecAttribute(Type type, String value) {
        String attributeName = Expectations.word().read(new StringCharReader(value));
        if (attributeName.isEmpty()) {
            throw new SyntaxException("Expected two values {attribute (space) value} but only got " + value);
        }
        String attrValue = value.replaceFirst(attributeName + "(,|=|:| )", "");
        return new SpecAttribute(attributeName, type, attrValue);
    }

    public SpecInside getSpecInside(String objectName, String value, Boolean isPartly) {
        SpecInside spec = new SpecInside(objectName, Parser.parseLocation(value));
        spec.setPartly(isPartly);
        return spec;
    }

    public SpecNear getSpecNear(String objectName, String value) {
        List<Location> locations = Parser.parseLocation(value);
        if (locations == null || locations.isEmpty()) {
            throw new SyntaxException("There is no location defined");
        }
        return new SpecNear(objectName, Parser.parseLocation(value));
    }

    public SpecAbove getSpecAbove(String objectName, String value) {
        return new SpecAbove(objectName, Parser.parseRange(value));
    }

    public SpecBelow getSpecBelow(String objectName, String value) {
        return new SpecBelow(objectName, Parser.parseRange(value));
    }

    public SpecLeftOf getSpecLeftOf(String objectName, String value) {
        return new SpecLeftOf(objectName, Parser.parseRange(value));
    }

    public SpecRightOf getSpecRightOf(String objectName, String value) {
        return new SpecRightOf(objectName, Parser.parseRange(value));
    }

    public SpecHorizontally getSpecHorizontally(String objectName, String value) {
        return (SpecHorizontally) processAlignment(objectName, value, "horizontally");
    }

    public SpecVertically getSpecVertically(String objectName, String value) {
        return (SpecVertically) processAlignment(objectName, value, "vertically");
    }

    private Spec processAlignment(String objectName, String value, String type) {
        StringCharReader reader = new StringCharReader(value);
        String[] words = ExpectWord.readAllWords(reader);
        Alignment alignment = Alignment.ALL;
        int errorRate = 0;
        if (words.length == 1) {
            errorRate = Parser.parseInt(words[0]);
            if (errorRate == 0) {
                alignment = Alignment.parse(words[0]);
            }
        } else if (words.length == 2) {
            alignment = Alignment.parse(words[0]);
            errorRate = Parser.parseInt(words[1]);
        }

        switch (type) {
            case "horizontally":
                if (alignment.isOneOf(CENTERED, TOP, BOTTOM, ALL)) {
                    return new SpecHorizontally(alignment, objectName).withErrorRate(errorRate);
                } else {
                    throw new SyntaxException("Horizontal alignment doesn't allow this side: " + alignment.toString());
                }
            case "vertically":
                if (alignment.isOneOf(CENTERED, LEFT, RIGHT, ALL)) {
                    return new SpecVertically(alignment, objectName).withErrorRate(errorRate);
                } else {
                    throw new SyntaxException("Verticall alignment doesn't allow this side: " + alignment.toString());
                }
            default:
                throw new SyntaxException("Unknown alignment: " + type);
        }
    }

    public SpecCentered getSpecCentered(String objectName, String value, SpecCentered.Location location, SpecCentered.Alignment alignment) {
        int errorRate = Parser.parseRange(value).getFrom().asInt();
        errorRate = errorRate == -1 ? 2 : errorRate;
        return new SpecCentered(objectName, alignment, location).withErrorRate(errorRate);
    }

    public SpecOn getSpecOn(String objectName, Side sideHorizontal, Side sideVertical, String value) {
        List<Location> locations = Parser.parseLocation(value);
        if (locations == null || locations.isEmpty()) {
            throw new SyntaxException("There is no location defined");
        }
        return new SpecOn(objectName, sideHorizontal, sideVertical, locations);
    }

    public SpecColorScheme getSpecColorScheme(String value) {
        List<ColorRange> colorRanges = Parser.parseColorRanges(value);
        if (colorRanges == null || colorRanges.isEmpty()) {
            throw new SyntaxException("There are no colors defined");
        }
        SpecColorScheme spec = new SpecColorScheme();
        spec.setColorRanges(colorRanges);
        return spec;
    }

    public SpecImage getSpecImage(String pageName, String objectName, String value) {
        SpecImage spec = new SpecImage();
        spec.setImagePaths(getImagepath(pageName, objectName));
        spec.setErrorRate(GalenConfig.getConfig().getImageSpecDefaultErrorRate());
        spec.setTolerance(GalenConfig.getConfig().getImageSpecDefaultTolerance());
        getImageParameters(spec, value);
        return spec;
    }

    private void getImageParameters(SpecImage spec, String Data) {
        List<Pair<String, String>> parameters = Expectations.commaSeparatedRepeatedKeyValues().read(new StringCharReader(Data));
        for (Pair<String, String> parameter : parameters) {
            if (null != parameter.getKey()) {
                switch (parameter.getKey()) {
                    case "file":
                        spec.getImagePaths().add(parameter.getValue());
                        break;
                    case "error":
                        spec.setErrorRate(SpecImage.ErrorRate.fromString(parameter.getValue()));
                        break;
                    case "tolerance":
                        spec.setTolerance(parseIntegerParameter("tolerance", parameter.getValue()));
                        break;
                    case "stretch":
                        spec.setStretch(true);
                        break;
                    case "area":
                        spec.setSelectedArea(parseRect(parameter.getValue()));
                        break;
                    case "filter": {
                        ImageFilter filter = parseImageFilter(parameter.getValue());
                        spec.getOriginalFilters().add(filter);
                        spec.getSampleFilters().add(filter);
                    }
                    break;
                    case "filter-a": {
                        ImageFilter filter = parseImageFilter(parameter.getValue());
                        spec.getOriginalFilters().add(filter);
                        break;
                    }
                    case "filter-b": {
                        ImageFilter filter = parseImageFilter(parameter.getValue());
                        spec.getSampleFilters().add(filter);
                    }
                    break;
                    case "map-filter": {
                        ImageFilter filter = parseImageFilter(parameter.getValue());
                        spec.getMapFilters().add(filter);
                    }
                    break;
                    case "crop-if-outside":
                        spec.setCropIfOutside(true);
                        break;
                    case "exclude-objects":
                        String ignoreObjects = parseExcludeObjects(parameter.getValue());
                        Optional.ofNullable(spec.getIgnoredObjectExpressions())
                                .orElseGet(() -> {
                                    List<String> l = new LinkedList<>();
                                    spec.setIgnoredObjectExpressions(l);
                                    return l;
                                })
                                .add(ignoreObjects);
                        break;
                    default:
                        throw new SyntaxException("Unknown parameter: " + parameter.getKey());
                }
            }
        }
    }

    private String parseExcludeObjects(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private Rect parseRect(String text) {
        Integer[] numbers = new Integer[4];

        StringCharReader reader = new StringCharReader(text);
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = new ExpectNumber().read(reader).intValue();
        }

        return new Rect(numbers);
    }

    private Integer parseIntegerParameter(String name, String value) {
        if (StringUtils.isNumeric(value)) {
            return Integer.parseInt(value);
        } else {
            throw new SyntaxException(name + " parameter should be integer: " + value);
        }
    }

    private List<String> getImagepath(String pageName, String objectName) {
        List<String> path = new ArrayList<>();
        File[] files = new File(FilePath.getORimagestorelocation() + File.separator + pageName + File.separator + objectName).listFiles();
        if (files == null || files.length == 0) {
            return path;
        }
        for (File file : files) {
            if (file.isFile()) {
                path.add(file.getAbsolutePath());
            } else {
                for (File file2 : file.listFiles()) {
                    if (file2.isFile()) {
                        path.add(file2.getAbsolutePath());
                    }
                }
            }
        }
        return path;
    }

    private ImageFilter parseImageFilter(String filterText) {
        StringCharReader reader = new StringCharReader(filterText);

        String filterName = new ExpectWord().read(reader);
        Double value = new ExpectNumber().read(reader);

        if (null != filterName) {
            switch (filterName) {
                case "contrast":
                    return new ContrastFilter(value.intValue());
                case "blur":
                    return new BlurFilter(value.intValue());
                case "denoise":
                    return new DenoiseFilter(value.intValue());
                case "saturation":
                    return new SaturationFilter(value.intValue());
                case "quantinize":
                    return new QuantinizeFilter(value.intValue());
            }
        }
        throw new SyntaxException("Unknown image filter: " + filterName);
    }
}

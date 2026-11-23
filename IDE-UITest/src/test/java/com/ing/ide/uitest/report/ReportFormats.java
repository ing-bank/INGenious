package com.ing.ide.uitest.report;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Report formats selectable with {@code -Duitest.report=<format>[,<format>...]}; default {@code html}.
 */
public final class ReportFormats {
    /** System property that selects the formats. */
    public static final String PROPERTY = "uitest.report";

    private static final String DEFAULT_FORMAT = "html";

    private static final Map<String, Supplier<ReportWriter>> AVAILABLE = new LinkedHashMap<>();

    static {
        AVAILABLE.put("html", HtmlReportWriter::new);
        AVAILABLE.put("rtm", RtmReportWriter::new);
    }

    private ReportFormats() {}

    /**
     * @return writers for the formats in {@value #PROPERTY}
     * @throws IllegalArgumentException if a format is unknown
     */
    public static List<ReportWriter> fromSystemProperty() {
        final String value = System.getProperty(PROPERTY, "").trim();
        return Arrays
            .stream((value.isEmpty() ? DEFAULT_FORMAT : value).split(","))
            .map(f -> f.trim().toLowerCase(Locale.ROOT))
            .filter(f -> !f.isEmpty())
            .distinct()
            .map(ReportFormats::writerFor)
            .collect(Collectors.toList());
    }

    private static ReportWriter writerFor(final String format) {
        final Supplier<ReportWriter> writer = AVAILABLE.get(format);
        if (writer == null) {
            throw new IllegalArgumentException(
                "Unknown report format '" +
                format +
                "' in -D" +
                PROPERTY +
                ". Available: " +
                String.join(", ", AVAILABLE.keySet())
            );
        }
        return writer.get();
    }
}

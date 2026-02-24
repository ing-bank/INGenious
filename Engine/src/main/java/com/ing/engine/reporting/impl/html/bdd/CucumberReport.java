
package com.ing.engine.reporting.impl.html.bdd;

import com.ing.datalib.model.Attribute;
import com.ing.datalib.model.Attributes;
import com.ing.datalib.model.DataItem;
import com.ing.datalib.model.Meta;
import com.ing.datalib.model.ProjectInfo;
import com.ing.datalib.model.Tag;
import com.ing.engine.core.Control;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;

/**
 * CucumberReport is responsible for generating Cucumber-compatible JSON and HTML reports
 * from test execution data. It parses report data, groups executions by scenario, and
 * transforms them into Cucumber FeatureReport objects. It also provides methods to save
 * reports to files and convert between formats.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>Use {@link #toCucumberReport(String, File)} to generate a Cucumber JSON report from a JSON string.</li>
 *   <li>Use {@link #toCucumberReport(File, File)} to generate a Cucumber JSON report from a file.</li>
 *   <li>Use {@link #get()} to obtain the singleton instance.</li>
 * </ul>
 */
public class CucumberReport {

    private static final CucumberReport INS = new CucumberReport();

    private File bddReport;

    /**
     * Returns the singleton instance of CucumberReport wrapped in an Optional.
     * @return Optional containing the singleton CucumberReport instance
     */
    public static Optional<CucumberReport> get() {
        return Optional.of(INS);
    }

    /**
     * create cucumber-html-reports from the cucumber-json report
     *
     * @param cucumberJson cucumber-json report
     * @param project project name
     */
    private void toCucumberHtmlReport(File cucumberJson, String project) {
        //TO-DO: add your html implementation
    }

    /**
     * convert report to cucumber-json report
     *
     * @param report - report (json string)
     * @param bddReport - destination file
     * @throws Exception
     */
    public void toCucumberReport(String report, File bddReport) throws Exception {
        this.bddReport = bddReport;
        toCucumberReport(parseReport(report), bddReport);
    }

    /**
     * convert report to cucumber-json report
     *
     * @param reportData - report data
     * @param bddReport - destination file
     * @throws Exception
     */
    private void toCucumberReport(Report reportData, File bddReport) throws Exception {
        saveAs(bddReport, convert(reportData));
        CucumberReport.get().ifPresent(reporter -> reporter.toCucumberHtmlReport(bddReport, reportData.projectName));
    }

    /**
     * convert report to cucumber-json report
     *
     * @param report - report file
     * @param bddReport - destination file
     * @throws Exception
     */
    public void toCucumberReport(File report, File bddReport) throws Exception {
        toCucumberReport(parseReport(report), bddReport);
    }

    /**
     * Saves the Cucumber JSON report string to a file.
     * @param res Destination file
     * @param cucumberReport Cucumber JSON report string
     */
    private void saveAs(File res, String cucumberReport) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(res));) {
            pw.print(cucumberReport);
        } catch (IOException ex) {
            Logger.getLogger(CucumberReport.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Converts parsed report data to a Cucumber JSON string.
     * @param reportData Parsed report data
     * @return Cucumber JSON string
     * @throws Exception if conversion fails
     */
    private String convert(Report reportData) throws Exception {
        return gson().toJson(toCucumberReport(reportData));
    }

    /**
     * Returns a Gson instance with pretty printing enabled.
     * @return Gson instance
     */
    private static Gson gson() {
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Parses a report file into a Report object.
     * @param jsonFile Source report file
     * @return Parsed Report object
     * @throws Exception if parsing fails
     */
    private Report parseReport(File jsonFile) throws Exception {
        return gson().fromJson(new FileReader(jsonFile), Report.class);
    }

    /**
     * Parses a JSON string into a Report object.
     * @param report JSON string
     * @return Parsed Report object
     * @throws Exception if parsing fails
     */
    private Report parseReport(String report) throws Exception {
        return gson().fromJson(report, Report.class);
    }

    /**
     * Groups executions by scenario and converts them to FeatureReport objects.
     * @param reportData Parsed report data
     * @return List of FeatureReport objects
     */
    private List<FeatureReport> toCucumberReport(Report reportData) {
        return reportData.getEXECUTIONS().stream().collect(groupingBy(Report.Execution::getScenarioName))
                .entrySet().stream().map(To::FeatureReport).collect(toList());
    }

    /**
     * Static helper class for transforming report data into FeatureReport and related objects.
     */
    private static class To {

        /**
         * Converts a scenario entry to a FeatureReport object.
         * @param story Entry containing scenario name and executions
         * @return FeatureReport object
         */
        private static FeatureReport FeatureReport(Entry<String, List<Report.Execution>> story) {
            // Safely handle missing scenario
            Optional<Meta> scenarioOpt = project().findScenario(story.getKey());
            String desc = scenarioOpt.map(Meta::getDesc).orElse("No description available");
            int featureLine = -1;
            if (scenarioOpt.isPresent()) {
                featureLine = getLine(scenarioOpt.get().getAttributes(), "feature.line");
            }
            return new FeatureReport(
                story.getKey(),
                story.getKey(),
                desc,
                String.format("//TestPlan/%s.feature", story.getKey()),
                featureLine,
                story.getValue().stream().map(To::Element).collect(toList()),
                getTags(story.getKey())
            );
        }

        /**
         * Converts an execution to a FeatureReport.Element object.
         * @param exe Report.Execution object
         * @return FeatureReport.Element object
         */
        private static FeatureReport.Element Element(Report.Execution exe) {
            return new FeatureReport.Element(getKeyword(exe),
                    getName(exe.description, exe.testcaseName), exe.description,
                    getLine(findTC(exe.testcaseName, exe.scenarioName).getAttributes(), "feature.children.line"),
                    getSteps(exe), getTags(exe.testcaseName, exe.scenarioName));
        }

        /**
         * Retrieves the keyword for a scenario step.
         * @param exe Report.Execution object
         * @return Step keyword
         */
        private static String getKeyword(Report.Execution exe) {
            return findTC(exe.testcaseName, exe.scenarioName)
                    .getAttributes().find("feature.children.keyword").orElse(Attribute.create("#", "Scenario"))
                    .getValue();
        }

        /**
         * Retrieves the line number for a feature or step.
         * @param attrs Attributes object
         * @param key Attribute key
         * @return Line number
         */
        private static int getLine(Attributes attrs, String key) {
            return Integer.valueOf(attrs.find(key).orElse(Attribute.create("", "-1")).getValue());
        }

        /**
         * Retrieves reusable steps from an execution.
         * @param exe Report.Execution object
         * @return List of FeatureReport.Step objects
         */
        private static List<FeatureReport.Step> getSteps(Report.Execution exe) {
            return exe.getIterData().get(0).getSteps().stream()
                    .filter(By::Reusable).map(To::Step).collect(toList());
        }

        /**
         * Retrieves tags for a scenario.
         * @param scn Scenario name
         * @return List of FeatureReport.Tag objects
         */
        private static List<FeatureReport.Tag> getTags(String scn) {
            return findScn(scn).getTags().stream().map(To::Tag).collect(toList());
        }

        /**
         * Finds scenario metadata by name.
         * @param scn Scenario name
         * @return Meta object for the scenario
         */
        private static Meta findScn(String scn) {
            return project().findScenario(scn).orElse(Meta.scenario());
        }

        /**
         * Retrieves tags for a test case and scenario.
         * @param tc Test case name
         * @param scn Scenario name
         * @return List of FeatureReport.Tag objects
         */
        private static List<FeatureReport.Tag> getTags(String tc, String scn) {
            return findTC(tc, scn).getTags().stream().map(To::Tag).collect(toList());
        }

        /**
         * Finds test case data by test case and scenario name.
         * @param tc Test case name
         * @param scn Scenario name
         * @return DataItem object for the test case
         */
        private static DataItem findTC(String tc, String scn) {
            return project().getData().find(tc, scn).orElse(DataItem.create(tc));
        }

        /**
         * Retrieves the current project info.
         * @return ProjectInfo object
         */
        private static ProjectInfo project() {
            return Control.exe.getProject().getInfo();
        }

        /**
         * Converts a Report.Step to a FeatureReport.Step object.
         * @param r Report.Step object
         * @return FeatureReport.Step object
         */
        private static FeatureReport.Step Step(Report.Step r) {
            return new FeatureReport.Step(getName(r.description, RC(r.name)[1]),
                    Result(r),
                    getLine(findTC(RC(r.name)[1], RC(r.name)[0]).getAttributes(), "feature.children.step.line"))
                    .withMatch(new FeatureReport.Match(String.format("//TestPlan/%s/%s.csv", (Object[]) RC(r.name))))
                    .addEmbeddings(getDesc(r.data)).addEmbeddings(getImages(r.data));
        }

        /**
         * Converts description data to text embeddings for Cucumber report.
         * @param data Step data object
         * @return List of text FeatureReport.Embedding objects
         */
        private static List<FeatureReport.Embedding> getDesc(Object data) {
            return dataStream(data).map(Report.Data::getDescription).map(To::Pure)
                    .map(String::getBytes).map(To::Base64).map(To::TxtEmbedding)
                    .collect(toList());
        }

        /**
         * Converts image data to image embeddings for Cucumber report.
         * @param data Step data object
         * @return List of image FeatureReport.Embedding objects
         */
        private static List<FeatureReport.Embedding> getImages(Object data) {
            return dataStream(data).filter(By::Image)
                    .map(Report.Data::getLink).map(To::File).map(To::Byte)
                    .map(To::Base64).map(To::PngEmbedding)
                    .collect(toList());
        }

        /**
         * Flattens a list of step data objects into a stream of Report.Data.
         * @param o List of step data objects
         * @return Stream of Report.Data
         */
        private static Stream<Report.Data> dataStream(Object o) {
            return ((List<Object>) o).stream().flatMap(To::Data);
        }

        /**
         * Converts a step data object to a stream of Report.Data.
         * @param o Step data object
         * @return Stream of Report.Data
         */
        private static Stream<Report.Data> Data(Object o) {
            Object data = ((Map) o).get("data");
            if (data instanceof List) {
                return dataStream(data);
            } else {
                return Stream.of(gson().fromJson(gson().toJson(data), Report.Data.class));
            }
        }

        /**
         * Converts a Tag object to a FeatureReport.Tag.
         * @param t Tag object
         * @return FeatureReport.Tag object
         */
        private static FeatureReport.Tag Tag(Tag t) {
            return new FeatureReport.Tag(t.getValue());
        }

        /**
         * Converts a Report.Step to a FeatureReport.Result.
         * @param s Report.Step object
         * @return FeatureReport.Result object
         */
        private static FeatureReport.Result Result(Report.Step s) {
            return new FeatureReport.Result(milliToNano() * getDuration(s), Status(s.getStatus()));
        }

        /**
         * Creates a text embedding from a string.
         * @param s Text string
         * @return FeatureReport.Embedding object
         */
        public static FeatureReport.Embedding TxtEmbedding(String s) {
            return new FeatureReport.Embedding("text/html", s);
        }

        /**
         * Creates an image embedding from a string (base64 encoded image).
         * @param s Base64 encoded image string
         * @return FeatureReport.Embedding object
         */
        public static FeatureReport.Embedding PngEmbedding(String s) {
            return new FeatureReport.Embedding("image/jpeg", s);
        }

        /**
         * Splits a string by the first colon.
         * @param s Input string
         * @return Array of two strings
         */
        public static String[] RC(String s) {
            return s.split(":", 2);
        }

        /**
         * Cleans a string for embedding, removing special tags.
         * @param s Input string
         * @return Cleaned string
         */
        public static String Pure(String s) {
            return Objects.toString(s, "").replace("#CTAG", "");
        }

        /**
         * Encodes a byte array to a Base64 string.
         * @param d Byte array
         * @return Base64 encoded string
         */
        public static String Base64(byte[] d) {
            return java.util.Base64.getEncoder().encodeToString(d);
        }

        /**
         * Reads a file and returns its bytes.
         * @param f File object
         * @return Byte array of file contents
         */
        public static byte[] Byte(File f) {
            try {
                return Files.readAllBytes(f.toPath());
            } catch (IOException ex) {
                return new byte[0];
            }
        }

        /**
         * Resolves a file path relative to the report directory.
         * @param f File name
         * @return File object
         */
        private static File File(String f) {
            return new File(INS.bddReport.getParentFile(), f);
        }

        /**
         * Converts a step status string to Cucumber status.
         * @param status Step status string
         * @return "passed" or "failed"
         */
        private static String Status(String status) {
            return Objects.nonNull(status) && status.toLowerCase().startsWith("pass") ? "passed" : "failed";
        }

        /**
         * Determines the step name, preferring description if available.
         * @param desc Step description
         * @param name Step name
         * @return Step name or description
         */
        private static String getName(String desc, String name) {
            return Objects.nonNull(desc) && !desc.isEmpty() && !desc.equals("Test Run") ? desc : name;
        }

        /**
         * Returns the conversion factor from milliseconds to nanoseconds.
         * @return 1000000
         */
        private static int milliToNano() {
            return 1000000;
        }

        /**
         * Calculates the duration of a step in milliseconds.
         * @param s Report.Step object
         * @return Duration in milliseconds
         */
        private static long getDuration(Report.Step s) {
            try {
                if (s.startTime != null && s.endTime != null) {
                    return Math.max(1, parseTime(s.endTime) - parseTime(s.startTime));
                } else {
                    return calcDuration(s);
                }
            } catch (Exception e) {
                return 1l;
            }
        }

        /**
         * Calculates the duration of a step from its data.
         * @param step Report.Step object
         * @return Duration in milliseconds
         * @throws Exception if parsing fails
         */
        @SuppressWarnings("unchecked")
        private static long calcDuration(Report.Step step) throws Exception {
            List<Map<String, Object>> data = (List<Map<String, Object>>) step.data;
            if (data.size() > 1) {
                return Math.max(1,
                        getTime(data.get(data.size() - 1)) - getTime(data.get(0)));
            } else {
                return 1l;
            }
        }

        /**
         * Extracts the timestamp from a step data map.
         * @param step Step data map
         * @return Timestamp in milliseconds
         * @throws ParseException if parsing fails
         */
        private static long getTime(Map<String, Object> step) throws ParseException {
            return parseTime(((Map<String, String>) step.get("data"))
                    .get(Report.Step.StepInfo.tStamp.name()));
        }

        /**
         * Parses a timestamp string to milliseconds.
         * @param val Timestamp string
         * @return Time in milliseconds
         * @throws ParseException if parsing fails
         */
        private static long parseTime(String val) throws ParseException {
            return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.sss").parse(val).getTime();
        }

    }

    /**
     * Utility class for filtering and identifying reusable steps and image data in report processing.
     */
    private static class By {

        /**
         * Checks if a step is marked as reusable.
         * @param s Report.Step object
         * @return true if step type is "reusable", false otherwise
         */
        private static boolean Reusable(Report.Step s) {
            return "reusable".equals(s.type);
        }

        /**
         * Checks if a data object contains an image link.
         * @param d Report.Data object
         * @return true if link is not null, false otherwise
         */
        private static boolean Image(Report.Data d) {
            return d.link != null;
        }
    }

}

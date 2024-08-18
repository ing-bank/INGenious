
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

public class CucumberReport {

    private static final CucumberReport INS = new CucumberReport();

    private File bddReport;

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

    private void saveAs(File res, String cucumberReport) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(res));) {
            pw.print(cucumberReport);
        } catch (IOException ex) {
            Logger.getLogger(CucumberReport.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private String convert(Report reportData) throws Exception {
        return gson().toJson(toCucumberReport(reportData));
    }

    private static Gson gson() {
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    }

    private Report parseReport(File jsonFile) throws Exception {
        return gson().fromJson(new FileReader(jsonFile), Report.class);
    }

    private Report parseReport(String report) throws Exception {
        return gson().fromJson(report, Report.class);
    }

    private List<FeatureReport> toCucumberReport(Report reportData) {
        return reportData.getEXECUTIONS().stream().collect(groupingBy(Report.Execution::getScenarioName))
                .entrySet().stream().map(To::FeatureReport).collect(toList());
    }

    private static class To {

        private static FeatureReport FeatureReport(Entry<String, List<Report.Execution>> story) {
            return new FeatureReport(story.getKey(), story.getKey(),
                    project().findScenario(story.getKey()).get().getDesc(),
                    String.format("//TestPlan/%s.feature", story.getKey()),
                    getLine(project().findScenario(story.getKey()).get().getAttributes(), "feature.line"),
                    story.getValue().stream().map(To::Element).collect(toList()),
                    getTags(story.getKey()));
        }

        private static FeatureReport.Element Element(Report.Execution exe) {
            return new FeatureReport.Element(getKeyword(exe),
                    getName(exe.description, exe.testcaseName), exe.description,
                    getLine(findTC(exe.testcaseName, exe.scenarioName).getAttributes(), "feature.children.line"),
                    getSteps(exe), getTags(exe.testcaseName, exe.scenarioName));
        }

        private static String getKeyword(Report.Execution exe) {
            return findTC(exe.testcaseName, exe.scenarioName)
                    .getAttributes().find("feature.children.keyword").orElse(Attribute.create("#", "Scenario"))
                    .getValue();
        }

        private static int getLine(Attributes attrs, String key) {
            return Integer.valueOf(attrs.find(key).orElse(Attribute.create("", "-1")).getValue());
        }

        private static List<FeatureReport.Step> getSteps(Report.Execution exe) {
            return exe.getIterData().get(0).getSteps().stream()
                    .filter(By::Reusable).map(To::Step).collect(toList());
        }

        private static List<FeatureReport.Tag> getTags(String scn) {
            return findScn(scn).getTags().stream().map(To::Tag).collect(toList());
        }

        private static Meta findScn(String scn) {
            return project().findScenario(scn).orElse(Meta.scenario());
        }

        private static List<FeatureReport.Tag> getTags(String tc, String scn) {
            return findTC(tc, scn).getTags().stream().map(To::Tag).collect(toList());
        }

        private static DataItem findTC(String tc, String scn) {
            return project().getData().find(tc, scn).orElse(DataItem.create(tc));
        }

        private static ProjectInfo project() {
            return Control.exe.getProject().getInfo();
        }

        private static FeatureReport.Step Step(Report.Step r) {
            return new FeatureReport.Step(getName(r.description, RC(r.name)[1]),
                    Result(r),
                    getLine(findTC(RC(r.name)[1], RC(r.name)[0]).getAttributes(), "feature.children.step.line"))
                    .withMatch(new FeatureReport.Match(String.format("//TestPlan/%s/%s.csv", (Object[]) RC(r.name))))
                    .addEmbeddings(getDesc(r.data)).addEmbeddings(getImages(r.data));
        }

        private static List<FeatureReport.Embedding> getDesc(Object data) {
            return dataStream(data).map(Report.Data::getDescription).map(To::Pure)
                    .map(String::getBytes).map(To::Base64).map(To::TxtEmbedding)
                    .collect(toList());
        }

        private static List<FeatureReport.Embedding> getImages(Object data) {
            return dataStream(data).filter(By::Image)
                    .map(Report.Data::getLink).map(To::File).map(To::Byte)
                    .map(To::Base64).map(To::PngEmbedding)
                    .collect(toList());
        }

        private static Stream<Report.Data> dataStream(Object o) {
            return ((List<Object>) o).stream().flatMap(To::Data);
        }

        private static Stream<Report.Data> Data(Object o) {
            Object data = ((Map) o).get("data");
            if (data instanceof List) {
                return dataStream(data);
            } else {
                return Stream.of(gson().fromJson(gson().toJson(data), Report.Data.class));
            }
        }

        private static FeatureReport.Tag Tag(Tag t) {
            return new FeatureReport.Tag(t.getValue());
        }

        private static FeatureReport.Result Result(Report.Step s) {
            return new FeatureReport.Result(milliToNano() * getDuration(s), Status(s.getStatus()));
        }

        public static FeatureReport.Embedding TxtEmbedding(String s) {
            return new FeatureReport.Embedding("text/html", s);
        }

        public static FeatureReport.Embedding PngEmbedding(String s) {
            return new FeatureReport.Embedding("image/jpeg", s);
        }

        public static String[] RC(String s) {
            return s.split(":", 2);
        }

        public static String Pure(String s) {
            return Objects.toString(s, "").replace("#CTAG", "");
        }

        public static String Base64(byte[] d) {
            return java.util.Base64.getEncoder().encodeToString(d);
        }

        public static byte[] Byte(File f) {
            try {
                return Files.readAllBytes(f.toPath());
            } catch (IOException ex) {
                return new byte[0];
            }
        }

        private static File File(String f) {
            return new File(INS.bddReport.getParentFile(), f);
        }

        private static String Status(String status) {
            return Objects.nonNull(status) && status.toLowerCase().startsWith("pass") ? "passed" : "failed";
        }

        private static String getName(String desc, String name) {
            return Objects.nonNull(desc) && !desc.isEmpty() && !desc.equals("Test Run") ? desc : name;
        }

        private static int milliToNano() {
            return 1000000;
        }

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

        private static long getTime(Map<String, Object> step) throws ParseException {
            return parseTime(((Map<String, String>) step.get("data"))
                    .get(Report.Step.StepInfo.tStamp.name()));
        }

        private static long parseTime(String val) throws ParseException {
            return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.sss").parse(val).getTime();
        }

    }

    private static class By {

        private static boolean Reusable(Report.Step s) {
            return "reusable".equals(s.type);
        }

        private static boolean Image(Report.Data d) {
            return d.link != null;
        }
    }

}

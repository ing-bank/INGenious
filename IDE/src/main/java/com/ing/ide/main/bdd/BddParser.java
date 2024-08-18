
package com.ing.ide.main.bdd;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.model.Attribute;
import com.ing.datalib.model.DataItem;
import com.ing.datalib.model.Meta;
import com.ing.datalib.model.ProjectInfo;
import com.ing.datalib.model.Tag;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.ide.main.mainui.AppMainFrame;
import com.google.common.io.Files;
import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.ast.DocString;
import gherkin.ast.Examples;
import gherkin.ast.Feature;
import gherkin.ast.GherkinDocument;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Step;
import gherkin.ast.TableCell;
import gherkin.ast.TableRow;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 *
 */
public class BddParser {

    private final static Parser<GherkinDocument> GHERKIN_PARSER = new Parser<>(new AstBuilder());

    private final AppMainFrame sMainFrame;

    public BddParser(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
    }

    public void parse(File file) {
        if (file != null && file.exists()) {
            try {
                Feature feature = GHERKIN_PARSER.parse(new FileReader(file)).getFeature();
                String featureName = feature.getName();
                createTestCases(updateInfo(create(featureName), feature),
                        feature);
                File tp = new File(sMainFrame.getProject().getLocation(), "TestPlan");
                Files.copy(file, new File(tp, file.getName()));
            } catch (Exception ex) {
                Logger.getLogger(BddParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private List<Tag> toTag(List<gherkin.ast.Tag> tags) {
        return tags.stream().map(
                t -> t.getName()).map(t -> Tag.create(t)).collect(Collectors.toList());
    }

    private Scenario create(String name) {
        if (sMainFrame.getProject().getScenarioByName(name) == null) {
            return sMainFrame.getProject().addScenario(name);
        }
        return sMainFrame.getProject().getScenarioByName(name);
    }

    private Scenario updateInfo(Scenario scn, Feature feature) {
        String desc = feature.getDescription();
        List<Tag> tags = toTag(feature.getTags());
        Meta metaRes = pModel().findScenario(scn.getName())
                .orElseGet(() -> {
                    Meta meta = Meta.createScenario(scn.getName());
                    pModel().addMeta(meta);
                    return meta;
                });
        metaRes.setTags(tags);
        metaRes.getAttributes().add(Attribute.create("feature.line", feature.getLocation().getLine()));
        metaRes.setDesc(desc);
        return scn;
    }

    public void parse(String file) {
        parse(new File(file));
    }

    public ProjectInfo pModel() {
        return sMainFrame.getProject().getInfo();
    }

    private void createTestCases(Scenario scenario, Feature feature) {
        for (ScenarioDefinition scenarioDef : feature.getChildren()) {
            String scenDefName = scenarioDef.getName();
            TestCase testCase = updateInfo(createTestCase(scenario, scenDefName), scenarioDef);
            testCase.clearSteps();
            for (Step step : scenarioDef.getSteps()) {
                String reusableName = convert(step.getText());
                TestCase reusable = updateInfo(createReusable(create("StepDefinitions"), reusableName), step);
                if (reusable != null) {
                    reusable.addNewStep()
                            .setInput(getInputField(testCase.getName(), step.getText()))
                            .setDescription(getDescription(step));
                }
                testCase.addNewStep()
                        .asReusableStep("StepDefinitions", reusableName)
                        .setDescription(getDescription(step));
            }
            if (scenarioDef instanceof ScenarioOutline) {
                ScenarioOutline scOutline = (ScenarioOutline) scenarioDef;
                createTestData(testCase, scOutline.getExamples());
            }
        }
    }

    private TestCase createTestCase(Scenario scenario, String name) {
        TestCase testCase = scenario.getTestCaseByName(name);
        if (testCase == null) {
            testCase = scenario.addTestCase(name);
            sMainFrame.getTestDesign().getProjectTree().getTreeModel()
                    .addTestCase(testCase);
        }
        return testCase;
    }

    private List getTags(ScenarioDefinition sdef) {
        if (sdef instanceof ScenarioOutline) {
            return ((ScenarioOutline) sdef).getTags();
        } else if (sdef instanceof gherkin.ast.Scenario) {
            return ((gherkin.ast.Scenario) sdef).getTags();
        } else {
            return null;
        }
    }

    private TestCase updateInfo(TestCase tc, ScenarioDefinition sdef) {
        if (tc != null) {
            DataItem tcInfo = pModel().getData().find(tc.getName(), tc.getScenario().getName())
                    .orElseGet(() -> {
                        DataItem di = DataItem.createTestCase(tc.getName(), tc.getScenario().getName());
                        pModel().addData(di);
                        return di;
                    });
            List<gherkin.ast.Tag> tags = getTags(sdef);
            if (tags != null) {
                tcInfo.setTags(toTag(tags));
            }
            tcInfo.getAttributes().update(Attribute.create("feature.children.line", sdef.getLocation().getLine()));
            tcInfo.getAttributes().update(Attribute.create("feature.children.name", sdef.getName()));
            tcInfo.getAttributes().update(Attribute.create("description", sdef.getDescription()));
            tcInfo.getAttributes().update(Attribute.create("feature.children.keyword", sdef.getKeyword()));
            pModel().addData(tcInfo);
        }
        return tc;
    }

    private TestCase updateInfo(TestCase tc, Step step) {
        if (tc != null) {
            DataItem tcInfo = pModel().getData().find(tc.getName(), tc.getScenario().getName())
                    .orElseGet(() -> {
                        DataItem di = DataItem.createTestCase(tc.getName(), tc.getScenario().getName());
                        pModel().addData(di);
                        return di;
                    });

            tcInfo.getAttributes().update(Attribute.create("feature.children.step.line", step.getLocation().getLine()));
            tcInfo.getAttributes().update(Attribute.create("feature.children.step.text", step.getText()));
            tcInfo.getAttributes().update(Attribute.create("feature.children.step.argument", step.getArgument()));
            tcInfo.getAttributes().update(Attribute.create("feature.children.step.keyword", step.getKeyword()));
            pModel().addData(tcInfo);
        }
        return tc;
    }

    private void createTestData(TestCase testCase, List<Examples> examples) {
        for (Examples example : examples) {
            TestDataModel tdModel = createTestData(testCase.getName());
            List<String> columns = new ArrayList<>();
            for (TableCell tCell : example.getTableHeader().getCells()) {
                columns.add(tCell.getValue());
                tdModel.addColumn(tCell.getValue());
            }
            for (int i = 0; i < example.getTableBody().size(); i++) {
                TableRow tRow = example.getTableBody().get(i);
                tdModel.addRecord();
                Record record = tdModel.getRecords().get(i);
                record.setScenario(testCase.getScenario().getName());
                record.setTestcase(testCase.getName());
                record.setIteration("1");
                record.setSubIteration("" + (i + 1));
                for (int j = 0; j < tRow.getCells().size(); j++) {
                    tdModel.setValueAt(tRow.getCells().get(j).getValue(),
                            i,
                            tdModel.getColumnIndex(columns.get(j)));
                }
            }
        }
    }

    private String convert(String text) {
        text = text.trim().replaceAll("<", "-").replaceAll(">", "-").
                replaceAll("[^a-zA-Z0-9\\.\\-\\ ]", "_");
        return text.substring(0, Math.min(text.length(), 250));
    }

    private String getInputField(String dataSheetName, String text) {
        Matcher m = Pattern.compile("<(.+?)>")
                .matcher(text);
        String ms;
        if (m.find()) {
            ms = m.group(1);
            return dataSheetName + ":" + ms;
        }
        return "";
    }

    private String getDescription(Step step) {
        if (step.getArgument() != null && step.getArgument() instanceof DocString) {
            return ((DocString) step.getArgument()).getContent();
        }
        return step.getKeyword() + " " + step.getText();
    }

    private TestCase createReusable(Scenario scenario, String name) {
        TestCase testCase = scenario.getTestCaseByName(name);
        if (testCase == null) {
            testCase = scenario.addTestCase(name);
            sMainFrame.getTestDesign().getReusableTree().getTreeModel()
                    .addTestCase(testCase);
            return testCase;
        }
        return null;
    }

    private TestDataModel createTestData(String name) {
        TestDataModel tdModel = sMainFrame.getProject()
                .getTestData()
                .defData()
                .getByName(name);

        if (tdModel == null) {
            tdModel = sMainFrame.getProject()
                    .getTestData()
                    .defData()
                    .addTestData();
            tdModel.rename(name);
            tdModel.removeColumn("Data1");
            tdModel.removeColumn("Data2");
            String enVName = sMainFrame.getProject()
                    .getTestData()
                    .defEnv();
            sMainFrame.getTestDesign().getTestDatacomp().testDataAdded(enVName, tdModel);
        }
        return tdModel;
    }

    private void addToTestSet(TestSet testSet, TestCase testCase) {
        testSet.addNewStep()
                .setTestScenario(testCase.getScenario().getName())
                .setTestCase(testCase.getName());
    }

    public void openEditor() {
        Stream.of(new File("Tools").listFiles()).filter((File f) -> f.getName().toLowerCase().contains("storywriter"))
                .findFirst().ifPresent((File sw) -> {
                    try {
                        Runtime.getRuntime().exec("java -jar  Tools/" + sw.getName());
                    } catch (Exception ex) {
                        Logger.getLogger(BddParser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
    }

}

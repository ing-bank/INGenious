package com.ing.ide.main.ui.search;

import com.ing.datalib.component.*;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.ide.settings.IconSettings;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;

public class SearchService {

    private final Project project;
    private final IconSettings iconSettings;

    public SearchService(Project project) {
        this.project = project;
        this.iconSettings = IconSettings.getIconSettings();
    }

    public List<SearchResult> search(String query) {
        List<SearchResult> results = new ArrayList<>();

        if (project == null || query == null || query.trim().isEmpty()) {
            return results;
        }

        String lowerQuery = query.toLowerCase().trim();

        searchTestCases(lowerQuery, results);

        searchReusables(lowerQuery, results);

        searchTestData(lowerQuery, results);

        searchObjectRepositories(lowerQuery, results);

        return results;
    }

    private void searchTestCases(String query, List<SearchResult> results) {
        for (Scenario scenario : project.getScenarios()) {
            for (TestCase testCase : scenario.getTestCases()) {
                if (!testCase.isReusable()) {
                    String testCaseName = testCase.getName();
                    if (testCaseName.toLowerCase().contains(query)) {
                        String path = "TestPlan/" + scenario.getName() + "/" + testCaseName;
                        results.add(new SearchResult(
                                testCaseName,
                                path,
                                SearchResult.FileType.TEST_CASE,
                                iconSettings.getTestPlanTestCase()
                        ));
                    }
                }
            }
        }
    }

    private void searchReusables(String query, List<SearchResult> results) {
        for (Scenario scenario : project.getScenarios()) {
            for (TestCase testCase : scenario.getTestCases()) {
                if (testCase.isReusable()) {
                    String reusableName = testCase.getName();
                    if (reusableName.toLowerCase().contains(query)) {
                        String path = "Reusable/" + scenario.getName() + "/" + reusableName;
                        results.add(new SearchResult(
                                reusableName,
                                path,
                                SearchResult.FileType.REUSABLE,
                                iconSettings.getReusableTestCase()
                        ));
                    }
                }
            }
        }
    }

    private void searchTestData(String query, List<SearchResult> results) {
        EnvTestData envTestData = project.getTestData();
        if (envTestData == null) {
            return;
        }

        for (TestData testData : envTestData.getAllEnvironments()) {
            String environment = testData.getEnviroment();

            if (testData.getGlobalData() != null) {
                String globalDataName = testData.getGlobalData().getName();
                if (globalDataName.toLowerCase().contains(query)) {
                    results.add(new SearchResult(
                            globalDataName,
                            environment + "/" + globalDataName,
                            SearchResult.FileType.TEST_DATA,
                            iconSettings.getTestPlanRoot()
                    ));
                }
            }

            for (TestDataModel testDataModel : testData.getTestDataList()) {
                String testDataName = testDataModel.getName();
                if (testDataName.toLowerCase().contains(query)) {
                    results.add(new SearchResult(
                            testDataName,
                            environment + "/" + testDataName,
                            SearchResult.FileType.TEST_DATA,
                            iconSettings.getTestPlanRoot()
                    ));
                }
            }
        }
    }

    private void searchObjectRepositories(String query, List<SearchResult> results) {
        ObjectRepository objectRepository = project.getObjectRepository();
        if (objectRepository == null) {
            return;
        }

        if (objectRepository.getWebOR() != null) {
            WebOR webOR = objectRepository.getWebOR();

            for (WebORPage page : webOR.getPages()) {
                String pageName = page.getName();

                if (pageName.toLowerCase().contains(query)) {
                    results.add(new SearchResult(
                            pageName,
                            "OR/" + pageName,
                            SearchResult.FileType.OBJECT_REPOSITORY,
                            iconSettings.getORPage()
                    ));
                }

                for (ObjectGroup<WebORObject> group : page.getObjectGroups()) {
                    for (WebORObject object : group.getObjects()) {
                        String objectName = object.getName();
                        if (objectName.toLowerCase().contains(query)) {
                            results.add(new SearchResult(
                                    objectName,
                                    "OR/" + pageName + "/" + objectName,
                                    SearchResult.FileType.OBJECT_REPOSITORY,
                                    iconSettings.getORObject()
                            ));
                        }
                    }
                }
            }
        }

        if (objectRepository.getMobileOR() != null) {
            MobileOR mobileOR = objectRepository.getMobileOR();

            for (MobileORPage page : mobileOR.getPages()) {
                String pageName = page.getName();

                if (pageName.toLowerCase().contains(query)) {
                    results.add(new SearchResult(
                            pageName,
                            "MOR/" + pageName,
                            SearchResult.FileType.OBJECT_REPOSITORY,
                            iconSettings.getORPage()
                    ));
                }

                for (ObjectGroup<MobileORObject> group : page.getObjectGroups()) {
                    for (MobileORObject object : group.getObjects()) {
                        String objectName = object.getName();
                        if (objectName.toLowerCase().contains(query)) {
                            results.add(new SearchResult(
                                    objectName,
                                    "MOR/" + pageName + "/" + objectName,
                                    SearchResult.FileType.OBJECT_REPOSITORY,
                                    iconSettings.getORObject()
                            ));
                        }
                    }
                }
            }
        }
    }
}

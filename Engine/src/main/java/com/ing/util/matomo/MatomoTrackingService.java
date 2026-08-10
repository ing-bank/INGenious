package com.ing.util.matomo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.engine.constants.FilePath;
import com.ing.util.matomo.config.MatomoConfig;
import com.ing.util.matomo.service.MatomoSender;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for tracking INGenious test execution metrics to Matomo.
 * Detects execution environment (Azure DevOps pipeline or local) and sends appropriate metrics.
 */
public class MatomoTrackingService {

    public static class MissingRequiredProfileFields {
        private final boolean sutMissing;
        private final boolean pcodeMissing;

        public MissingRequiredProfileFields(boolean sutMissing, boolean pcodeMissing) {
            this.sutMissing = sutMissing;
            this.pcodeMissing = pcodeMissing;
        }

        public boolean isSutMissing() {
            return sutMissing;
        }

        public boolean isPcodeMissing() {
            return pcodeMissing;
        }

        public boolean hasMissingValues() {
            return sutMissing || pcodeMissing;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MatomoTrackingService.class);

    // Azure DevOps environment variables
    private static final String BUILD_REPOSITORY_NAME = "BUILD_REPOSITORY_NAME";
    private static final String BUILD_REQUESTED_FOR = "BUILD_REQUESTEDFOR";
    private static final String SYSTEM_TEAMPROJECT = "SYSTEM_TEAMPROJECT";
    private static final String AGENT_NAME = "AGENT_NAME";
    private static final String TF_BUILD = "TF_BUILD";

    // Event configuration
    private static final String EVENT_CATEGORY = "Test Execution";
    private int PCODE_DIMENSION_ID;
    private int SUT_DIMENSION_ID;
    private int EXEC_ENV_DIMENSION_ID;

    private final MatomoSender sender;
    private final ObjectMapper objectMapper;
    private final String configLocation;
    private final String userProfilePath;

    /**
     * Constructs MatomoTrackingService with default configuration.
     */
    public MatomoTrackingService(String configLocation, String userProfileLocation) {
        MatomoConfig config = new MatomoConfig(configLocation);
        this.sender = new MatomoSender(config);
        this.objectMapper = new ObjectMapper();
        this.configLocation = configLocation;
        this.userProfilePath = userProfileLocation;

        this.PCODE_DIMENSION_ID = Integer.parseInt(config.getProperty("pcodeDimensionID"));
        this.SUT_DIMENSION_ID = Integer.parseInt(config.getProperty("SUTDimensionID"));
        this.EXEC_ENV_DIMENSION_ID =
            Integer.parseInt(config.getProperty("executionEnvDimensionID"));
        // logger.info("MatomoTrackingService initialized");
    }

    /**
     * Constructs MatomoTrackingService with provided sender (for testing).
     *
     * @param sender the Matomo sender instance
     */
    public MatomoTrackingService(MatomoSender sender) {
        this.sender = sender;
        this.objectMapper = new ObjectMapper();
        this.configLocation = null;
        this.userProfilePath = null;
    }

    /**
     * Tracks a test execution event to Matomo.
     * Detects whether running in Azure DevOps pipeline or locally.
     */
    public void trackTestExecution() {
        try {
            ExecutionContext context = detectExecutionContext();

            if (context == null) {
                logger.warn("Could not detect execution context, skipping Matomo tracking");
                return;
            }

            String json = buildTrackingJson(context);

            // logger.info("Tracking test execution: pcode={}, environment={}",
            //            context.pcode, context.isAzureDevOps ? "Azure DevOps" : "Local");

            boolean success = sender.sendMetrics(json);
            // if (success) {
            //     logger.info("Test execution tracked successfully to Matomo");
            // } else {
            //     logger.warn("Failed to track test execution to Matomo");
            // }

        } catch (Exception e) {
            logger.error("Error tracking test execution to Matomo", e);
        }
    }

    /**
     * Validates that userProfile.properties contains non-empty SUT and pcode values.
     *
     * @return true when both fields are present and non-empty; otherwise false
     */
    public boolean hasRequiredUserProfileValues() {
        return !getMissingRequiredProfileFields().hasMissingValues();
    }

    public MissingRequiredProfileFields getMissingRequiredProfileFields() {
        boolean isPipelineExecution = isAzureDevOpsPipelineExecution();
        Properties properties = loadUserProfileProperties();
        if (properties == null) {
            return new MissingRequiredProfileFields(true, !isPipelineExecution);
        }

        String sut = properties.getProperty("SUT");
        String pcode = properties.getProperty("pcode");

        boolean sutMissing = sut == null || sut.trim().isEmpty();
        boolean pcodeMissing = !isPipelineExecution && (pcode == null || pcode.trim().isEmpty());

        if (sutMissing) {
            logger.error("SUT property not found in profile properties file");
        }

        if (pcodeMissing) {
            logger.error("pcode property not found in profile properties file");
        }

        return new MissingRequiredProfileFields(sutMissing, pcodeMissing);
    }

    /**
     * Detects the execution context (Azure DevOps or local).
     *
     * @return ExecutionContext with pcode and sut, or null if detection fails
     */
    private ExecutionContext detectExecutionContext() {
        // Check if running in Azure DevOps pipeline
        if (isAzureDevOpsPipelineExecution()) {
            return detectAzureDevOpsContext();
        } else {
            return detectLocalContext();
        }
    }

    private boolean isAzureDevOpsPipelineExecution() {
        String tfBuild = System.getenv(TF_BUILD);
        return "True".equalsIgnoreCase(tfBuild);
    }

    /**
     * Detects execution context from Azure DevOps environment variables.
     *
     * @return ExecutionContext for Azure DevOps, or null if detection fails
     */
    private ExecutionContext detectAzureDevOpsContext() {
        // logger.info("Detected Azure DevOps pipeline execution");

        Properties properties = loadUserProfileProperties();
        if (properties == null) {
            return null;
        }

        String sut = properties.getProperty("SUT");
        if (sut == null || sut.trim().isEmpty()) {
            logger.warn("SUT property not found in profile properties file");
            return null;
        }

        // Get repository name and extract first 6 characters as pcode
        String repoName = System.getenv(BUILD_REPOSITORY_NAME);
        if (repoName == null || repoName.isEmpty()) {
            logger.warn("BUILD_REPOSITORY_NAME environment variable not found");
            return null;
        }

        String pcode = extractPcodeFromRepoName(repoName);

        // logger.info("Azure DevOps context: pcode={}", pcode);

        ExecutionContext context = new ExecutionContext();
        context.pcode = pcode;
        context.sut = sut;
        context.executionEnvironment = "pipelines -" + System.getenv(AGENT_NAME);
        context.isAzureDevOps = true;

        return context;
    }

    /**
     * Extracts pcode from repository name (first 6 characters).
     *
     * @param repoName the repository name
     * @return pcode (first 6 characters, lowercase)
     */
    private String extractPcodeFromRepoName(String repoName) {
        if (repoName.length() >= 6) {
            return repoName.substring(0, 6).toLowerCase();
        } else {
            logger.warn("Repository name '{}' is shorter than 6 characters", repoName);
            return repoName.toLowerCase();
        }
    }

    /**
     * Detects execution context from local profile properties file.
     *
     * @return ExecutionContext for local execution, or null if detection fails
     */
    private ExecutionContext detectLocalContext() {
        // logger.info("Detected local execution");
        Properties properties = loadUserProfileProperties();
        if (properties == null) {
            return null;
        }

        String pcode = properties.getProperty("pcode");
        String sut = properties.getProperty("SUT");

        if (pcode == null || pcode.trim().isEmpty()) {
            logger.warn("pcode property not found in profile properties file");
            return null;
        }

        if (sut == null || sut.trim().isEmpty()) {
            logger.warn("SUT property not found in profile properties file");
            return null;
        }

        // logger.info("Local context from profile properties: pcode={}, SUT={}", pcode, sut);

        ExecutionContext context = new ExecutionContext();
        context.pcode = pcode;
        context.sut = sut;
        context.executionEnvironment = "local - " + System.getProperty("os.name");
        context.isAzureDevOps = false;

        return context;
    }

    private Properties loadUserProfileProperties() {
        if (userProfilePath == null || userProfilePath.isEmpty()) {
            logger.warn("User profile path is not configured");
            return null;
        }

        Path profilePath = Paths.get(userProfilePath);
        if (!Files.exists(profilePath)) {
            logger.warn("Profile properties file not found at {}", userProfilePath);
            return null;
        }

        try (InputStream inputStream = Files.newInputStream(profilePath)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            logger.error("Error reading profile properties file", e);
            return null;
        }
    }

    // /**
    //  * Finds the profile.json file in the Configuration directory.
    //  * Searches relative to the current working directory.
    //  *
    //  * @return Path to profile.json, or null if not found
    //  */
    // private Path findProfileJson() {
    //     // Try multiple potential locations
    //     String[] locations = {
    //         "Configuration/profile.json",
    //         "../Configuration/profile.json",
    //         "../../Configuration/profile.json",
    //         "Resources/Configuration/profile.json",
    //         "../Resources/Configuration/profile.json"
    //     };

    //     for (String location : locations) {
    //         Path path = Paths.get(location);
    //         if (Files.exists(path)) {
    //             logger.info("Found profile.json at: {}", path.toAbsolutePath());
    //             return path;
    //         }
    //     }

    //     logger.warn("profile.json not found in any expected location");
    //     return null;
    // }

    /**
     * Builds the JSON tracking payload for Matomo.
     *
     * @param context the execution context
     * @return JSON string for Matomo tracking
     */
    private String buildTrackingJson(ExecutionContext context) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("EventCategory", EVENT_CATEGORY);
        payload.put("EventAction", "Test run");
        // payload.put("EventAction", context.isAzureDevOps ? "azure-devops" : "local");

        List<Map<String, Object>> customDimensions = new ArrayList<>();
        addCustomDimension(customDimensions, PCODE_DIMENSION_ID, context.pcode);
        addCustomDimension(customDimensions, SUT_DIMENSION_ID, context.sut);
        addCustomDimension(customDimensions, EXEC_ENV_DIMENSION_ID, context.executionEnvironment);

        payload.put("customDimensions", customDimensions);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            logger.error("Error building JSON payload", e);
            return null;
        }
    }

    private void addCustomDimension(
        List<Map<String, Object>> customDimensions,
        int id,
        String value
    ) {
        if (id <= 0 || value == null || value.trim().isEmpty()) {
            return;
        }

        Map<String, Object> customDimension = new HashMap<>();
        customDimension.put("id", id);
        customDimension.put("value", value);
        customDimensions.add(customDimension);
    }

    /**
     * Closes the sender and releases resources.
     */
    public void close() {
        if (sender != null) {
            sender.close();
        }
    }

    /**
     * Internal class to hold execution context information.
     */
    private static class ExecutionContext {
        String pcode;
        String sut;
        String executionEnvironment;
        boolean isAzureDevOps;
    }
}

package com.ing.util.matomo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.util.matomo.config.MatomoConfig;
import org.matomo.java.tracking.*;
import org.matomo.java.tracking.parameters.VisitorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for sending INGenious adoption KPI metrics to Matomo.
 * This class handles JSON input conversion and Matomo event tracking.
 */
public class MatomoSender {

    private static final Logger logger = LoggerFactory.getLogger(MatomoSender.class);
    
    private final MatomoTracker tracker;
    private final MatomoConfig config;
    private final ObjectMapper objectMapper;

    // /**
    //  * Constructs MatomoSender with default configuration loaded from matomo.properties.
    //  *
    //  * @throws IllegalStateException if configuration cannot be loaded
    //  */
    // public MatomoSender() {
    //     this(new MatomoConfig());
    // }

    /**
     * Constructs MatomoSender with provided configuration.
     *
     * @param config the Matomo configuration
     */
    public MatomoSender(MatomoConfig config) {
        this.config = config;
        
        // Build tracker configuration using the official SDK pattern
        TrackerConfiguration trackerConfig = TrackerConfiguration.builder()
                .apiEndpoint(URI.create(config.getMatomoUrl()))
                .defaultSiteId(config.getSiteId())
                .disableSslCertValidation(true)
                .build();
        
        // logger.info("tracker Matomo URL={}", trackerConfig.getApiEndpoint());
        // logger.info("tracker Matomo Site ID={}", trackerConfig.getDefaultSiteId());
        
        this.tracker = new MatomoTracker(trackerConfig);
        this.objectMapper = new ObjectMapper();
        
        // logger.info("MatomoSender initialized with config: {}", config);
    }

    /**
     * Sends adoption metrics to Matomo from JSON string input.
     * Required keys: EventCategory, EventAction, visitorID
     * Optional keys: EventName, EventValue, customDimensions
     *
     * @param jsonInput JSON string containing adoption metrics
     * @return true if event was sent successfully, false otherwise
     * @throws IOException if JSON parsing fails
     */
    public boolean sendMetrics(String jsonInput) throws IOException {
        if (jsonInput == null || jsonInput.trim().isEmpty()) {
            logger.error("JSON input is null or empty");
            return false;
        }

        // Parse JSON into JsonNode for flexible schema handling
        JsonNode jsonNode = objectMapper.readTree(jsonInput);
        
        // Validate required fields
        String eventCategory = validateRequiredField(jsonNode, "EventCategory");
        if (eventCategory == null) return false;
        
        String eventAction = validateRequiredField(jsonNode, "EventAction");
        if (eventAction == null) return false;
        
        // String visitorID = validateRequiredField(jsonNode, "visitorID");
        // if (visitorID == null) return false;
        
        // Extract optional fields
        String eventName = extractOptionalString(jsonNode, "EventName");
        Double eventValue = extractOptionalDouble(jsonNode, "EventValue");
        Map<Integer, String> customDimensions = extractCustomDimensions(jsonNode);
        if (customDimensions == null) return false; // validation failed
        
        // Build and send request
        try {
            MatomoRequest request = buildMatomoRequest(
                eventCategory, 
                eventAction, 
                eventName, 
                eventValue, 
                // visitorID, 
                customDimensions
            );
            
            // logger.info("Sending event to Matomo: category={}, action={}, name={}", 
            //            eventCategory, eventAction, eventName);
            
            tracker.sendBulkRequestAsync(request).join();
            
            
            logger.info("Event sent successfully to Matomo");
            return true;
            
        } catch (Exception e) {
            // Extract root cause for detailed error info
            Throwable cause = e;
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
            }
            
            // Extract HTTP code from error message if present
            String errorMsg = cause.getMessage();
            String httpCode = "Unknown";
            if (errorMsg != null && errorMsg.contains("code ")) {
                int idx = errorMsg.indexOf("code ") + 5;
                int endIdx = errorMsg.indexOf(" ", idx);
                if (endIdx == -1) endIdx = errorMsg.indexOf(")", idx);
                if (endIdx == -1) endIdx = errorMsg.length();
                httpCode = errorMsg.substring(idx, endIdx).trim();
            }
            
            logger.error("Matomo request failed - HTTP {}: {} ({})", 
                        httpCode, errorMsg, cause.getClass().getSimpleName());
            logger.error("Full exception details:", e);
            
            return false;
        }
    }
    
    /**
     * Validates a required field exists and is non-blank.
     *
     * @param jsonNode the JSON node to extract from
     * @param fieldName the field name to validate
     * @return the field value if valid, null otherwise
     */
    private String validateRequiredField(JsonNode jsonNode, String fieldName) {
        if (!jsonNode.has(fieldName)) {
            logger.error("Required field '{}' is missing from JSON input", fieldName);
            return null;
        }
        
        JsonNode fieldNode = jsonNode.get(fieldName);
        if (fieldNode.isNull()) {
            logger.error("Required field '{}' is null", fieldName);
            return null;
        }
        
        String value = fieldNode.asText();
        if (value == null || value.trim().isEmpty()) {
            logger.error("Required field '{}' is blank", fieldName);
            return null;
        }
        
        return value;
    }
    
    /**
     * Extracts an optional string field from JSON.
     *
     * @param jsonNode the JSON node to extract from
     * @param fieldName the field name
     * @return the field value or null if not present
     */
    private String extractOptionalString(JsonNode jsonNode, String fieldName) {
        if (!jsonNode.has(fieldName) || jsonNode.get(fieldName).isNull()) {
            return null;
        }
        return jsonNode.get(fieldName).asText();
    }
    
    /**
     * Extracts an optional double field from JSON.
     *
     * @param jsonNode the JSON node to extract from
     * @param fieldName the field name
     * @return the field value or null if not present
     */
    private Double extractOptionalDouble(JsonNode jsonNode, String fieldName) {
        if (!jsonNode.has(fieldName) || jsonNode.get(fieldName).isNull()) {
            return null;
        }
        return jsonNode.get(fieldName).asDouble();
    }
    
    /**
     * Extracts and validates custom dimensions from JSON.
     * Expected format: [{"id": 1, "value": "p33148"}, {"id": 2, "value": "dev"}]
     *
     * @param jsonNode the JSON node to extract from
     * @return map of dimension ID to value, or null if validation fails
     */
    private Map<Integer, String> extractCustomDimensions(JsonNode jsonNode) {
        if (!jsonNode.has("customDimensions") || jsonNode.get("customDimensions").isNull()) {
            return new HashMap<>();
        }
        
        JsonNode dimensionsNode = jsonNode.get("customDimensions");
        if (!dimensionsNode.isArray()) {
            logger.error("customDimensions must be an array");
            return null;
        }
        
        Map<Integer, String> dimensions = new HashMap<>();
        for (JsonNode dimNode : dimensionsNode) {
            if (!dimNode.has("id") || dimNode.get("id").isNull()) {
                logger.error("Custom dimension missing 'id' field: {}", dimNode);
                return null;
            }
            if (!dimNode.has("value") || dimNode.get("value").isNull()) {
                logger.error("Custom dimension missing 'value' field: {}", dimNode);
                return null;
            }
            
            int id = dimNode.get("id").asInt();
            String value = dimNode.get("value").asText();
            dimensions.put(id, value);
            logger.debug("Extracted custom dimension: id={}, value={}", id, value);
        }
        
        return dimensions;
    }

    /**
     * Builds a MatomoRequest from raw parameters.
     *
     * @param eventCategory the event category
     * @param eventAction the event action
     * @param eventName the event name (optional, may be null)
     * @param eventValue the event value (optional, may be null)
     * @param visitorID the visitor ID
     * @param customDimensions map of dimension ID to value
     * @return configured MatomoRequest
     */
    private MatomoRequest buildMatomoRequest(
            String eventCategory,
            String eventAction,
            String eventName,
            Double eventValue,
            // String visitorID,
            Map<Integer, String> customDimensions) {
        
        // Use MatomoRequests.event() as recommended by the SDK documentation
        MatomoRequest.MatomoRequestBuilder builder = MatomoRequests.event(
                eventCategory,
                eventAction,
                eventName,
                eventValue
        );

        // Add custom dimensions using additionalParameters Map
        if (customDimensions != null && !customDimensions.isEmpty()) {
            Map<String, Object> additionalParams = new HashMap<>();
            
            for (Map.Entry<Integer, String> entry : customDimensions.entrySet()) {
                // Add custom dimension as additional parameter
                // Format: dimension<id> = value
                String paramKey = "dimension" + entry.getKey();
                additionalParams.put(paramKey, entry.getValue());
                logger.debug("Added custom dimension {}: {}", entry.getKey(), entry.getValue());
            }
            
            builder.additionalParameters(additionalParams);
        }
        
        // Set visitor ID
        // builder.visitorId(VisitorId.fromString(visitorID));

        MatomoRequest request = builder.build();

        logger.debug("siteId={}", request.getSiteId());
        logger.debug("url={}", request.getActionUrl());
        logger.debug("request={}", request);
        logger.debug("eventCategory={}", request.getEventCategory());
        logger.debug("eventAction={}", request.getEventAction());
        logger.debug("eventName={}", request.getEventName());
        // logger.debug("visitorId={}", visitorID);

        return request;
    }
    
    /**
     * Sends a batch of adoption metrics to Matomo.
     *
     * @param jsonInputs array of JSON strings containing adoption metrics
     * @return number of successfully sent events
     */
    public int sendMetricsBatch(String[] jsonInputs) {
        if (jsonInputs == null || jsonInputs.length == 0) {
            logger.warn("No metrics to send");
            return 0;
        }

        int successCount = 0;
        for (String jsonInput : jsonInputs) {
            try {
                if (sendMetrics(jsonInput)) {
                    successCount++;
                }
            } catch (IOException e) {
                logger.error("Failed to parse JSON input: {}", jsonInput, e);
            }
        }

        // logger.info("Sent {}/{} events successfully", successCount, jsonInputs.length);
        return successCount;
    }

    /**
     * No-op method for compatibility.
     * MatomoTracker is stateless and does not require explicit cleanup.
     */
    public void close() {
        // logger.info("MatomoSender close called (no action required - tracker is stateless)");
    }
}

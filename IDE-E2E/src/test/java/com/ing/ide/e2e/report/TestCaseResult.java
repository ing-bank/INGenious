package com.ing.ide.e2e.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Result of one test case, as stored in the run folder and passed to report writers.
 */
public final class TestCaseResult {
    @JsonProperty
    private final TestIdentity identity;

    @JsonProperty
    private final TestStatus status;

    @JsonProperty
    private final String startedAt;

    @JsonProperty
    private final long durationMs;

    @JsonProperty
    private final String message;

    @JsonProperty
    private final List<Evidence> evidence;

    /**
     * @param identity scenario and test case codes and descriptions
     * @param status outcome
     * @param startedAt ISO-8601 start time
     * @param durationMs duration in milliseconds
     * @param message failure or skip reason, {@code null} when passed
     * @param evidence screenshots and recordings, in capture order
     */
    @JsonCreator
    public TestCaseResult(
        @JsonProperty("identity") final TestIdentity identity,
        @JsonProperty("status") final TestStatus status,
        @JsonProperty("startedAt") final String startedAt,
        @JsonProperty("durationMs") final long durationMs,
        @JsonProperty("message") final String message,
        @JsonProperty("evidence") final List<Evidence> evidence
    ) {
        this.identity = identity;
        this.status = status;
        this.startedAt = startedAt;
        this.durationMs = durationMs;
        this.message = message;
        this.evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    /**
     * @return scenario and test case codes and descriptions
     */
    public TestIdentity identity() {
        return identity;
    }

    /**
     * @return outcome
     */
    public TestStatus status() {
        return status;
    }

    /**
     * @return ISO-8601 start time
     */
    public String startedAt() {
        return startedAt;
    }

    /**
     * @return duration in milliseconds
     */
    public long durationMs() {
        return durationMs;
    }

    /**
     * @return failure or skip reason, {@code null} when passed
     */
    public String message() {
        return message;
    }

    /**
     * @return screenshots and recordings, in capture order
     */
    public List<Evidence> evidence() {
        return evidence;
    }
}

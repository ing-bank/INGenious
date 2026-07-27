package com.ing.ingenious.api.contract.ui;

/**
 * Contract for a plugin that already knows which test case a recording belongs to.
 *
 * <p>When a recording starts, Studio has to decide where the recorded steps will go, and with
 * no other source of truth it asks the user: new scenario, current test case, or an existing
 * one. That question is unavoidable for a standalone Studio, but it is redundant — and easy to
 * answer wrongly — whenever the user has <em>already</em> said what they are working on
 * somewhere else. Teams wire Studio to a story tracker, a requirements tool, or their own test
 * management, pick an item there, and then have to say it a second time to the recorder.
 *
 * <p>Implement this on a plugin entry class (the same class listed in the JAR manifest's
 * {@code pluginEntryClasses} attribute) and Studio asks the plugin first. Return a target and
 * the recorder uses it, creating the scenario and test case when they do not exist yet, and
 * opening the test case in the editor. Return {@code null} and the user is asked as usual, so a
 * plugin only speaks up when it genuinely has an answer.
 *
 * <pre>{@code
 * public class StoryRecordingTarget implements RecordingTargetApi {
 *     public RecordingTarget getRecordingTarget() {
 *         Story story = myTracker.currentStory();
 *         return story == null ? null : new RecordingTarget(story.epic(), story.title());
 *     }
 *     }
 * }</pre>
 *
 * <p>Implementations must have a public no-argument constructor. The method is called on the
 * Event Dispatch Thread each time a recording starts, so it must return promptly and must not
 * block; it is called once per recording rather than cached, which lets a plugin follow whatever
 * the user last selected without notifying Studio. Throwing is treated as "no answer" and the
 * user is asked, so a failing plugin can never stop a recording from starting.
 */
public interface RecordingTargetApi {
    /**
     * The test case the next recording belongs to.
     *
     * @return the target, or {@code null} to let the user choose as usual
     */
    RecordingTarget getRecordingTarget();
}

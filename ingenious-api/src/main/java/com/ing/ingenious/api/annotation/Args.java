package com.ing.ingenious.api.annotation;

import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Companion to {@link Action} that declares the <em>format</em> of an action's
 * input and condition. Kept separate from {@code @Action} so existing
 * annotations are untouched and adoption is incremental.
 *
 * <p>Authored on the command method next to the code that consumes
 * {@code getInput()} / {@code getCondition()} at runtime, so the spec can never
 * silently drift from the implementation. Consumed by
 * {@code ActionSpecCatalog} which the IDE IntelliSense and the AI
 * step-authoring tools both read.
 *
 * <pre>
 * &#64;Action(object = ObjectType.WEBSERVICE, input = YES, condition = OPTIONAL,
 *         desc = "Assert the HTTP response status code")
 * &#64;Args(input = ArgType.HTTP_STATUS, inputExample = "@200",
 *       condition = ConditionKind.ALIAS_API, conditionExample = "#PetStore",
 *       help = "Expected HTTP status code; prefix literals with @")
 * public void assertResponseCode() { ... }
 * </pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Args {
    /** Grammar of the primary input value. */
    ArgType input() default ArgType.TEXT;

    /** Canonical example input, e.g. {@code "@200"}. */
    String inputExample() default "";

    /**
     * Whether the input may be a data-driven reference ({@code Sheet:Column},
     * {@code %var%}, {@code =function}). {@code false} forces a literal only.
     */
    boolean inputAllowsData() default true;

    /** Grammar of the second object value (for two-object actions). */
    ArgType secondObject() default ArgType.TEXT;

    /** Semantic category of the condition field. */
    ConditionKind condition() default ConditionKind.NONE;

    /** Allowed condition values when {@link #condition()} is {@code ENUM}. */
    String[] conditionValues() default {};

    /** Canonical example condition, e.g. {@code "#PetStore"}. */
    String conditionExample() default "";

    /** One-line human hint surfaced in IDE tooltips and {@code action_info}. */
    String help() default "";

    /**
     * Field-specific hint for the <b>Input</b> column, shown as ghost text in the
     * IDE when the cell is empty. Falls back to the {@link #input()} type label +
     * {@link #inputExample()} when blank. Use this when the {@code ArgType} alone
     * is ambiguous (e.g. an assertion's Input is the <i>expected value</i>).
     */
    String inputHelp() default "";

    /**
     * Field-specific hint for the <b>Condition</b> column, shown as ghost text in
     * the IDE when the cell is empty. Falls back to a hint derived from
     * {@link #condition()} / {@link #conditionExample()} when blank. Use this when
     * the {@code ConditionKind} alone is ambiguous (e.g. a JSONPath, or an
     * optional timeout in ms).
     */
    String conditionHelp() default "";
}

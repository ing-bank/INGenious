package com.ing.ingenious.api.types;

/**
 * The semantic category of an action's {@code Condition} field. Drives the
 * IDE condition dropdown and the AI/lint validation of the condition value.
 *
 * @see com.ing.ingenious.api.annotation.Args
 */
public enum ConditionKind {
    /** Action takes no condition. */
    NONE,
    /** Parameter markers ({@code Start Param} / {@code End Param}). */
    PARAM,
    /** Loop markers ({@code Start Loop} / {@code End Loop:@n}). */
    LOOP,
    /** An {@code #apiAlias} selecting the API context. */
    ALIAS_API,
    /** A {@code #contextAlias} selecting a context. */
    ALIAS_CONTEXT,
    /** One of a fixed set of values supplied by {@code conditionValues}. */
    ENUM,
    /** Viewport target ({@code screen} / {@code viewport}). */
    VIEWPORT,
    /** {@code GlobalObject} marker. */
    GLOBAL_OBJECT,
    /** Free text condition. */
    TEXT;
}

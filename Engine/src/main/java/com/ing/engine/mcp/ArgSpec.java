package com.ing.engine.mcp;

import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The resolved input/condition specification for a single action. Built from an
 * {@link Args @Args} annotation (or a sidecar override) when available, or
 * inferred as free text otherwise.
 *
 * <p>Holds the two deterministic behaviours shared by the IDE and the AI
 * step-authoring tools:
 * <ul>
 *   <li>{@link #normalize(String, String)} - deterministic auto-correction plus
 *       hard-error reporting for values that are unambiguously wrong for the
 *       action's {@link ArgType} / {@link ConditionKind}.</li>
 *   <li>{@link #validate(String, String)} - a pure check used by the linter and
 *       the IDE inline validator.</li>
 * </ul>
 *
 * <p>Only {@link #isExplicit() explicit} specs (from {@code @Args} or a sidecar)
 * ever raise errors; inferred specs preserve the legacy, tolerant behaviour so
 * un-annotated actions are never rejected.
 */
public final class ArgSpec {
    private final String action;
    private final ArgType inputType;
    private final String inputExample;
    private final boolean inputAllowsData;
    private final ArgType secondObjectType;
    private final ConditionKind conditionKind;
    private final List<String> conditionValues;
    private final String conditionExample;
    private final String help;
    private final String inputHelp;
    private final String conditionHelp;
    private final boolean explicit;

    ArgSpec(
        String action,
        ArgType inputType,
        String inputExample,
        boolean inputAllowsData,
        ArgType secondObjectType,
        ConditionKind conditionKind,
        List<String> conditionValues,
        String conditionExample,
        String help,
        String inputHelp,
        String conditionHelp,
        boolean explicit
    ) {
        this.action = action;
        this.inputType = inputType == null ? ArgType.TEXT : inputType;
        this.inputExample = inputExample == null ? "" : inputExample;
        this.inputAllowsData = inputAllowsData;
        this.secondObjectType = secondObjectType == null ? ArgType.TEXT : secondObjectType;
        this.conditionKind = conditionKind == null ? ConditionKind.NONE : conditionKind;
        this.conditionValues =
            conditionValues == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(conditionValues));
        this.conditionExample = conditionExample == null ? "" : conditionExample;
        this.help = help == null ? "" : help;
        this.inputHelp = inputHelp == null ? "" : inputHelp;
        this.conditionHelp = conditionHelp == null ? "" : conditionHelp;
        this.explicit = explicit;
    }

    // ------------------------------------------------------------------
    // factories
    // ------------------------------------------------------------------

    /** Build a spec from an {@code @Args} annotation. */
    static ArgSpec fromAnnotation(String action, Args a) {
        return new ArgSpec(
            action,
            a.input(),
            a.inputExample(),
            a.inputAllowsData(),
            a.secondObject(),
            a.condition(),
            Arrays.asList(a.conditionValues()),
            a.conditionExample(),
            a.help(),
            a.inputHelp(),
            a.conditionHelp(),
            true
        );
    }

    /**
     * Free-text fallback spec for an action without an explicit spec. Payload
     * actions (postRestRequest, ...) infer a raw body so their input is never
     * {@code @}-prefixed, preserving the legacy normalization behaviour.
     */
    static ArgSpec inferred(String action) {
        ArgType t = ConventionCatalog.isPayloadAction(action) ? ArgType.JSON_BODY : ArgType.TEXT;
        return new ArgSpec(
            action,
            t,
            "",
            true,
            ArgType.TEXT,
            ConditionKind.NONE,
            Collections.<String>emptyList(),
            "",
            "",
            "",
            "",
            false
        );
    }

    // ------------------------------------------------------------------
    // accessors
    // ------------------------------------------------------------------

    public String action() {
        return action;
    }

    public ArgType inputType() {
        return inputType;
    }

    public String inputExample() {
        return inputExample;
    }

    public boolean inputAllowsData() {
        return inputAllowsData;
    }

    public ArgType secondObjectType() {
        return secondObjectType;
    }

    public ConditionKind conditionKind() {
        return conditionKind;
    }

    public List<String> conditionValues() {
        return conditionValues;
    }

    public String conditionExample() {
        return conditionExample;
    }

    public String help() {
        return help;
    }

    /** Field-specific Input hint (may be empty; prefer {@link #inputHint()}). */
    public String inputHelp() {
        return inputHelp;
    }

    /** Field-specific Condition hint (may be empty; prefer {@link #conditionHint()}). */
    public String conditionHelp() {
        return conditionHelp;
    }

    /**
     * Human hint for the Input column: the explicit {@link #inputHelp} when set,
     * otherwise derived from the {@link ArgType} label and example. Never null.
     */
    public String inputHint() {
        if (!inputHelp.isEmpty()) {
            return inputHelp;
        }
        String base = inputType.label();
        return inputExample.isEmpty() ? base : base + " (e.g. " + inputExample + ")";
    }

    /**
     * Human hint for the Condition column: the explicit {@link #conditionHelp}
     * when set, otherwise derived from the {@link ConditionKind} and example.
     * Empty when the action takes no condition. Never null.
     */
    public String conditionHint() {
        if (!conditionHelp.isEmpty()) {
            return conditionHelp;
        }
        switch (conditionKind) {
            case ALIAS_API:
                return "optional #apiAlias" + eg(conditionExample);
            case ALIAS_CONTEXT:
                return "optional #contextAlias" + eg(conditionExample);
            case ALIAS_KAFKA_PRODUCER:
                return "optional #producerAlias" + eg(conditionExample);
            case ALIAS_KAFKA_CONSUMER:
                return "optional #consumerAlias" + eg(conditionExample);
            case ENUM:
                return conditionValues.isEmpty()
                    ? ""
                    : "one of: " + String.join(", ", conditionValues);
            case VIEWPORT:
                return "screen | viewport";
            case PARAM:
                return "Start Param / End Param";
            case LOOP:
                return "Start Loop / End Loop:@n";
            case GLOBAL_OBJECT:
                return "GlobalObject";
            case TEXT:
                return conditionExample.isEmpty() ? "" : "e.g. " + conditionExample;
            case NONE:
            default:
                return "";
        }
    }

    private static String eg(String example) {
        return example == null || example.isEmpty() ? "" : " (e.g. " + example + ")";
    }

    /** True when this spec came from {@code @Args} or a sidecar (not inferred). */
    public boolean isExplicit() {
        return explicit;
    }

    // ------------------------------------------------------------------
    // normalization
    // ------------------------------------------------------------------

    /** Outcome of {@link #normalize(String, String)}. */
    public static final class Result {
        public final String input;
        public final String condition;
        public final List<String> warnings;
        public final List<String> errors;

        Result(String input, String condition, List<String> warnings, List<String> errors) {
            this.input = input;
            this.condition = condition;
            this.warnings = warnings;
            this.errors = errors;
        }
    }

    /**
     * Deterministically corrects {@code rawInput}/{@code rawCondition} for this
     * action. Known-safe fixes are applied; values that are unambiguously wrong
     * for a strict {@link ArgType}/{@link ConditionKind} are reported as errors
     * (only for {@link #isExplicit() explicit} specs).
     */
    public Result normalize(String rawInput, String rawCondition) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String in = normalizeInput(rawInput, warnings, errors);
        String cond = normalizeCondition(rawCondition, warnings, errors);
        return new Result(in, cond, warnings, errors);
    }

    private String normalizeInput(String rawInput, List<String> warnings, List<String> errors) {
        String in = rawInput == null ? "" : rawInput;
        if (in.isEmpty()) {
            return in;
        }
        // Raw bodies (SQL / JSON / XML) are never @-prefixed or validated as literals.
        if (inputType.isBody()) {
            return in;
        }
        // Dynamic references are always accepted as-is.
        if (isDynamicReference(in)) {
            return in;
        }
        // A concrete @literal: unwrap and validate against the type.
        if (in.startsWith("@") && in.length() > 1) {
            String literal = in.substring(1);
            if (explicit && inputType.isStrict() && !inputType.matchesLiteral(literal)) {
                errors.add(inputTypeError(in));
            }
            return in;
        }
        // Bare literal: @-prefix it (legacy behaviour) and validate.
        if (explicit && inputType.isStrict() && !inputType.matchesLiteral(in)) {
            errors.add(inputTypeError(in));
        }
        return "@" + in;
    }

    private String normalizeCondition(
        String rawCondition,
        List<String> warnings,
        List<String> errors
    ) {
        String cond = rawCondition == null ? "" : rawCondition;
        if (!explicit || cond.trim().isEmpty()) {
            return cond;
        }
        String trimmed = cond.trim();
        if (conditionKind == ConditionKind.ENUM) {
            // Accept a matching allowed value (canonicalising case) or a dynamic ref.
            if (isDynamicReference(trimmed)) {
                return cond;
            }
            for (String v : conditionValues) {
                if (v.equalsIgnoreCase(trimmed)) {
                    return v; // canonical case
                }
            }
            errors.add(
                action +
                " condition must be one of " +
                conditionValues +
                (conditionExample.isEmpty() ? "" : " (e.g. " + conditionExample + ")") +
                ". Got '" +
                cond +
                "'."
            );
        } else if (
            conditionKind == ConditionKind.ALIAS_API ||
            conditionKind == ConditionKind.ALIAS_CONTEXT ||
            conditionKind == ConditionKind.ALIAS_KAFKA_PRODUCER ||
            conditionKind == ConditionKind.ALIAS_KAFKA_CONSUMER
        ) {
            if (!trimmed.startsWith("#") && !isDynamicReference(trimmed)) {
                warnings.add(
                    action +
                    " condition should be a " +
                    aliasLabel(conditionKind) +
                    (conditionExample.isEmpty() ? "" : " (e.g. " + conditionExample + ")") +
                    "; got '" +
                    cond +
                    "'."
                );
            }
        }
        return cond;
    }

    private static String aliasLabel(ConditionKind kind) {
        switch (kind) {
            case ALIAS_API:
                return "#apiAlias";
            case ALIAS_CONTEXT:
                return "#contextAlias";
            case ALIAS_KAFKA_PRODUCER:
                return "#producerAlias";
            case ALIAS_KAFKA_CONSUMER:
                return "#consumerAlias";
            default:
                return "#alias";
        }
    }

    private String inputTypeError(String got) {
        return (
            action +
            " expects input of type " +
            inputType.label() +
            (inputExample.isEmpty() ? "" : " (e.g. " + inputExample + ")") +
            ". Got '" +
            got +
            "'."
        );
    }

    // ------------------------------------------------------------------
    // validation (pure - used by the linter / IDE renderer)
    // ------------------------------------------------------------------

    /**
     * Pure validation of a stored step. Returns human-readable violation
     * messages (empty when valid). Never mutates. Only explicit specs validate.
     */
    public List<String> validate(String input, String condition) {
        List<String> out = new ArrayList<>();
        if (!explicit) {
            return out;
        }
        String in = input == null ? "" : input;
        if (
            !in.isEmpty() && !inputType.isBody() && !isDynamicReference(in) && inputType.isStrict()
        ) {
            String literal = in.startsWith("@") ? in.substring(1) : in;
            if (!inputType.matchesLiteral(literal)) {
                out.add(inputTypeError(in));
            }
        }
        String cond = condition == null ? "" : condition.trim();
        if (!cond.isEmpty()) {
            if (conditionKind == ConditionKind.ENUM && !isDynamicReference(cond)) {
                boolean ok = false;
                for (String v : conditionValues) {
                    if (v.equalsIgnoreCase(cond)) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    out.add(action + " condition must be one of " + conditionValues + ".");
                }
            }
        }
        return out;
    }

    /**
     * A value that is not a concrete literal: a data reference, embedded payload
     * token, runtime variable, function, alias or engine directive. Such values
     * are resolved at runtime and are always accepted.
     */
    private static boolean isDynamicReference(String v) {
        if (v == null || v.isEmpty()) {
            return false;
        }
        String t = v.trim();
        char c = t.charAt(0);
        if (c == '%' || c == '=' || c == '#' || c == '{') {
            return true;
        }
        if (ConventionCatalog.isDataRef(t)) {
            return true;
        }
        if (ConventionCatalog.containsPayloadTokens(t)) {
            return true;
        }
        if (ConventionCatalog.isEngineDirective(t)) {
            return true;
        }
        return false;
    }

    /** Lower-cased condition-kind label for docs/UI. */
    public String conditionKindLabel() {
        return conditionKind.name().toLowerCase(Locale.ROOT);
    }
}

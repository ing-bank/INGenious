package com.ing.engine.mcp;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes step fields at write time so every tool that creates or edits
 * steps ({@code testcase_create}, {@code testcase_add_step},
 * {@code testcase_insert_step}, {@code testcase_edit_step},
 * {@code gen_testcase}, importers, browser recording) produces steps that
 * follow the {@link ConventionCatalog} input grammar.
 *
 * <p>Normalization is deterministic and conservative:
 * <ul>
 *   <li>A bare literal input (not {@code @}/{@code #}/{@code %}/{@code {}}-prefixed
 *       and not a {@code Sheet:Column} data reference) is {@code @}-prefixed -
 *       except for payload-bearing actions whose input is the raw body.</li>
 *   <li>Inputs that already follow the grammar are never modified.</li>
 *   <li>Suspicious values ({@code #id} in an input, {@code @}-prefixed object)
 *       yield warnings but are not silently rewritten.</li>
 * </ul>
 */
final class StepNormalizer {

    private StepNormalizer() {}

    /** Outcome of normalizing one step. */
    static final class Result {
        final String input;
        final String condition;
        final List<String> warnings;
        final List<String> errors;

        Result(String input, String condition, List<String> warnings, List<String> errors) {
            this.input = input;
            this.condition = condition;
            this.warnings = warnings;
            this.errors = errors;
        }
    }

    /** Back-compat overload without a condition. */
    static Result normalize(String stepLabel, String action, String object, String input) {
        return normalize(stepLabel, action, object, input, "");
    }

    /**
     * Normalizes {@code input} and {@code condition} for {@code action} and
     * reports convention warnings (soft) and errors (hard) for the whole step.
     * The action's {@link ArgSpec} (from {@code @Args} or a sidecar) drives
     * type-specific auto-correction, validation and condition handling; only
     * explicit specs raise errors. {@code stepLabel} prefixes messages.
     */
    static Result normalize(
        String stepLabel,
        String action,
        String object,
        String input,
        String condition
    ) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String act = action == null ? "" : action.trim();
        String in = input == null ? "" : input;
        String obj = object == null ? "" : object.trim();

        // Object grammar: OR references are never @-prefixed (E7).
        if (obj.startsWith("@") && !ConventionCatalog.isEngineDirective(obj)) {
            warnings.add(
                stepLabel +
                ": object '" +
                obj +
                "' is @-prefixed; object references must not carry '@' [E7]"
            );
        }

        // GlobalData ids never belong in a step input (E6).
        if (!in.isEmpty() && ConventionCatalog.isGlobalDataId(in)) {
            warnings.add(
                stepLabel +
                ": input '" +
                in +
                "' is a GlobalData environment id; put #ids in data-sheet cells and " +
                "reference them as Sheet:Column [E6]"
            );
        }

        // Type-specific input/condition normalization + validation via the spec.
        ArgSpec spec = ActionSpecCatalog.forAction(act);
        ArgSpec.Result sr = spec.normalize(in, condition);
        for (String w : sr.warnings) {
            warnings.add(stepLabel + ": " + w);
        }
        for (String e : sr.errors) {
            errors.add(stepLabel + ": " + e);
        }
        return new Result(sr.input, sr.condition, warnings, errors);
    }

    /**
     * Aggregate note appended to write-tool results when hard-coded literals
     * were stored, steering the model toward the parameterize workflow (W1).
     * Returns {@code null} when there is nothing to report.
     */
    static String literalSummary(int literalCount) {
        if (literalCount <= 0) return null;
        return (
            literalCount +
            " step(s) use hard-coded @literal inputs. Best practice: externalise them " +
            "with ingenious_testcase_parameterize (mode=scan first, then apply) [W1]"
        );
    }
}

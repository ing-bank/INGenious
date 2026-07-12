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
        final List<String> warnings;

        Result(String input, List<String> warnings) {
            this.input = input;
            this.warnings = warnings;
        }
    }

    /**
     * Normalizes {@code input} for {@code action} and reports convention
     * warnings for the whole step. {@code stepLabel} is used to prefix
     * warning messages (e.g. {@code "step 3"}).
     */
    static Result normalize(String stepLabel, String action, String object, String input) {
        List<String> warnings = new ArrayList<>();
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

        if (!in.isEmpty()) {
            // GlobalData ids never belong in a step input (E6).
            if (ConventionCatalog.isGlobalDataId(in)) {
                warnings.add(
                    stepLabel +
                    ": input '" +
                    in +
                    "' is a GlobalData environment id; put #ids in data-sheet cells and " +
                    "reference them as Sheet:Column [E6]"
                );
            } else if (needsAtPrefix(act, in)) {
                in = "@" + in;
            }
        }
        return new Result(in, warnings);
    }

    /**
     * True when a bare literal must be {@code @}-prefixed: it is not already
     * grammar-conformant and the action does not take a raw payload body.
     */
    private static boolean needsAtPrefix(String action, String input) {
        if (ConventionCatalog.isPayloadAction(action)) return false;
        char c = input.charAt(0);
        if (c == '@' || c == '#' || c == '%' || c == '{') return false;
        if (ConventionCatalog.isDataRef(input)) return false;
        if (ConventionCatalog.containsPayloadTokens(input)) return false;
        return true;
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

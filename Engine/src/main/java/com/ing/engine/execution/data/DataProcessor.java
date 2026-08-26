package com.ing.engine.execution.data;

import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunManager;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.util.data.KeyMap;
import com.ing.engine.util.data.fx.FParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 */
public class DataProcessor {
    private static final Pattern RT_VAR = Pattern.compile("(%.+?%)");

    public static String trimFirst(String v) {
        if (v.length() > 1) {
            return v.substring(1);
        } else {
            return "";
        }
    }

    public static String resolve(Object raw) {
        String inp = resolveKeyMapVars(Objects.toString(raw, ""), 2);
        inp = resolveIn(inp);
        return resolveKeyMapVars(inp, 2);
    }

    /**
     * "[Shared] Sheet:Column" or "[Project] Sheet:Column" - an explicit Test Data scope tag,
     * bare or braced. Mirrors ReusableRef.Scope's [Project]/[Shared] convention. Checked first so
     * it can be carved out of the generic "starts with [" -> dynamic/literal-value rule below.
     */
    private static final String SCOPED_DATASHEET_PATTERN =
        "^\\{?(\\[Shared\\]|\\[Project\\])\\s*[^}\\d:][^}:]*:[^}\\d:][^}:]*\\}?$";

    private static boolean isScopedDataSheetRef(String inp) {
        return inp.matches(SCOPED_DATASHEET_PATTERN);
    }

    private static boolean isInputPatternDynamic(String inp) {
        return (
            inp.matches("(^@|=|>|%)(.*)") || // check if Static string | addVar function | Dynamic Variable
            inp.startsWith("<") || //
            (inp.startsWith("[") && !isScopedDataSheetRef(inp)) || //
            inp.matches("^\\{[^:\\d\\s][^:]*\\}") || // check if starts with curly braces with no colon inside
            inp.startsWith("\"")
        ); // check if starts with double quote
    }

    public static boolean isInputPatternDataSheet(String inp) {
        return (
            (
                !inp.startsWith("<") || //
                !inp.startsWith("[") || //
                !inp.matches("^\\{[^:\\d\\s][^:]*\\}")
            ) && // check if does not starts with curly braces with no colon inside
            (
                inp.matches("^[A-Za-z].*:[A-Za-z].*") || // check if DataSheet:Data
                inp.matches("^\\{[^}\\d:][^}:]*:[^}\\d:][^}:]*\\}") || // check if {DataSheet:Data}
                isScopedDataSheetRef(inp)
            ) // check if [Shared]/[Project] Sheet:Column, bare or braced
        );
    }

    public static synchronized String resolve(String raw, TestCaseRunner context, String subIter)
        throws DataNotFoundException {
        String inp = Objects.toString(raw, "");
        //resolveKeyMapVars(Objects.toString(raw, ""), 2, context.getControl().getRunTimeVars())
        if (isInputPatternDynamic(inp)) {
            inp = resolveDynamic(resolveIn(inp), context);
        } else if (isInputPatternDataSheet(inp)) {
            String inp_string = inp.startsWith("{") ? inp.substring(1, inp.length() - 1) : inp;
            String[] args = inp_string.split(":");
            if (!context.isIterResolved(args[0])) {
                context.setIter(args[0], DataAccess.getIterations(context, args[0]));
            }
            inp = DataAccess.getData(context, args[0], args[1], context.iteration(), subIter);
        }
        return resolveKeyMapVars(inp, 2, context.getControl().getRunTimeVars());
    }

    private static String resolveIn(String inp) {
        if (inp.startsWith("@")) {
            inp = trimFirst(inp);
        } else if (inp.startsWith("=")) {
            inp = Objects.toString(FParser.eval(trimFirst(inp)), "");
        } else if (inp.startsWith(">")) {
            inp = Objects.toString(FParser.evaljs(trimFirst(inp)), "");
        }
        return inp;
    }

    private static String resolveDynamic(String data, TestCaseRunner context) {
        Matcher matcher = RT_VAR.matcher(data);
        while (matcher.find()) {
            String var = matcher.group();
            String inp = context.getControl().getDynamicValue(var);
            if (inp != null) {
                System.out.println(String.format("%s changed to %s", var, inp));
                data = data.replace(var, inp);
            }
        }
        return data;
    }

    /**
     * A Global Data reference, e.g. "#url" or the explicitly-scoped "[Shared] #url"/"[Project]
     * #url" (mirrors DataAccessInternal's [Shared]/[Project] scope tag convention).
     */
    private static boolean isGlobalDataRef(String inp) {
        String t = Objects.toString(inp, "").trim();
        if (t.startsWith("#")) {
            return true;
        }
        if (t.startsWith(DataAccessInternal.SHARED_SCOPE_TAG)) {
            return t.substring(DataAccessInternal.SHARED_SCOPE_TAG.length()).trim().startsWith("#");
        }
        if (t.startsWith(DataAccessInternal.PROJECT_SCOPE_TAG)) {
            return t
                .substring(DataAccessInternal.PROJECT_SCOPE_TAG.length())
                .trim()
                .startsWith("#");
        }
        return false;
    }

    public static String resolveDynamicData(Object raw, TestCaseRunner context, String field)
        throws DataNotFoundException {
        String inp = resolveKeyMapVars(
            Objects.toString(raw, ""),
            2,
            context.getControl().getRunTimeVars()
        );
        inp = resolveDynamic(resolveIn(inp), context);
        if (isGlobalDataRef(inp)) {
            inp = DataAccess.getGlobalData(context, inp, field);
        }

        return resolveKeyMapVars(inp, 2, context.getControl().getRunTimeVars());
    }

    public static String resolve(Object raw, TestCaseRunner context, String field)
        throws DataNotFoundException {
        String inp = resolveKeyMapVars(
            Objects.toString(raw, ""),
            2,
            context.getControl().getRunTimeVars()
        );
        inp = resolveDynamic(resolveIn(inp), context);
        if (isGlobalDataRef(inp)) {
            inp = DataAccess.getGlobalData(context, inp, field);
        }

        return resolveKeyMapVars(inp, 2, context.getControl().getRunTimeVars());
    }

    public static String resolveKeyMapVars(String inp, int pass, Map<String, String> runTimeVars) {
        inp =
            KeyMap.replaceKeys(
                inp,
                KeyMap.USER_VARS,
                true,
                pass,
                runTimeVars,
                Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
            );
        inp =
            KeyMap.replaceKeys(
                inp,
                KeyMap.CONTEXT_VARS,
                true,
                pass,
                Control.exe.getExecSettings().getRunSettings(),
                Control.exe.getExecSettings().getTestMgmgtSettings(),
                Control.getCurrentProject().getProjectSettings().getTestMgmtModule().asMap(),
                RunManager.getGlobalSettings(),
                Control.getCurrentProject().getProjectSettings().getDriverSettings(),
                Control.getCurrentProject().getProjectSettings().getUserDefinedSettings(),
                SystemDefaults.EnvVars,
                SystemDefaults.CLVars
            );
        inp = KeyMap.resolveEnvVars(inp);
        return inp;
    }

    public static String resolveKeyMapVars(String inp, int pass) {
        return resolveKeyMapVars(inp, pass, new HashMap<>());
    }
}

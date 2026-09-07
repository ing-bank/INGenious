package com.ing.engine.aicli.ui;

import com.ing.engine.aicli.ai.AiProvider;
import com.ing.engine.aicli.conversation.SessionContext;
import com.ing.engine.constants.SystemDefaults;

/**
 * Prints the AI CLI startup banner — the same block-letter art as the
 * {@code ingenious} launcher, wrapped in an info section.
 *
 * <p>Kept in its own class so the long string-literal lines are isolated from
 * {@code Repl.java} (the prettier-java 0.7.0 formatter crashes on very long
 * lines, but is fine when they appear in a small dedicated source file).
 */
public final class AiBanner {

    private AiBanner() {}

    public static void print(Theme t, SessionContext session, AiProvider provider) {
        boolean ansi = t.ansiEnabled();
        String p = ansi ? "\u001b[38;2;119;36;255m" : ""; // #7724FF purple
        String b = ansi ? "\u001b[38;2;147;92;255m" : ""; // bright purple
        String l = ansi ? "\u001b[38;2;180;140;255m" : ""; // light purple
        String w = ansi ? "\u001b[38;2;255;255;255m" : ""; // white
        String r = ansi ? "\u001b[0m" : "";
        String bo = ansi ? "\u001b[1m" : "";
        String di = ansi ? "\u001b[2m" : "";

        System.out.println();

        // Row 1
        System.out.println(
            p +
            "    \u2588\u2588\u2557" +
            b +
            "\u2588\u2588\u2588\u2557   \u2588\u2588\u2557" +
            p +
            " \u2588\u2588\u2588\u2588\u2588\u2588\u2557 " +
            b +
            "\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557" +
            p +
            "\u2588\u2588\u2588\u2557   \u2588\u2588\u2557" +
            b +
            "\u2588\u2588\u2557" +
            p +
            " \u2588\u2588\u2588\u2588\u2588\u2588\u2557 " +
            b +
            "\u2588\u2588\u2557   \u2588\u2588\u2557" +
            p +
            "\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557" +
            r
        );

        // Row 2
        System.out.println(
            p +
            "    \u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551" +
            p +
            "\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255d " +
            b +
            "\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255d" +
            p +
            "\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2551" +
            p +
            "\u2588\u2588\u2554\u2550\u2550\u2550\u2588\u2588\u2557" +
            b +
            "\u2588\u2588\u2551   \u2588\u2588\u2551" +
            p +
            "\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255d" +
            r
        );

        // Row 3
        System.out.println(
            b +
            "    \u2588\u2588\u2551" +
            l +
            "\u2588\u2588\u2554\u2588\u2588\u2557 \u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2551  \u2588\u2588\u2588\u2557" +
            l +
            "\u2588\u2588\u2588\u2588\u2588\u2557  " +
            b +
            "\u2588\u2588\u2554\u2588\u2588\u2557 \u2588\u2588\u2551" +
            l +
            "\u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2551   \u2588\u2588\u2551" +
            l +
            "\u2588\u2588\u2551   \u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557" +
            r
        );

        // Row 4
        System.out.println(
            b +
            "    \u2588\u2588\u2551" +
            p +
            "\u2588\u2588\u2551\u255a\u2588\u2588\u2557\u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2551   \u2588\u2588\u2551" +
            p +
            "\u2588\u2588\u2554\u2550\u2550\u255d  " +
            b +
            "\u2588\u2588\u2551\u255a\u2588\u2588\u2557\u2588\u2588\u2551" +
            p +
            "\u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2551   \u2588\u2588\u2551" +
            p +
            "\u2588\u2588\u2551   \u2588\u2588\u2551" +
            b +
            "\u255a\u2550\u2550\u2550\u2550\u2588\u2588\u2551" +
            r
        );

        // Row 5
        System.out.println(
            l +
            "    \u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2551 \u255a\u2588\u2588\u2588\u2588\u2551" +
            l +
            "\u255a\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d" +
            b +
            "\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557" +
            l +
            "\u2588\u2588\u2551 \u255a\u2588\u2588\u2588\u2588\u2551" +
            b +
            "\u2588\u2588\u2551" +
            l +
            "\u255a\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d" +
            b +
            "\u255a\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d" +
            l +
            "\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551" +
            r
        );

        // Row 6
        System.out.println(
            p +
            "    \u255a\u2550\u255d" +
            l +
            "\u255a\u2550\u255d  \u255a\u2550\u2550\u2550\u255d" +
            p +
            " \u255a\u2550\u2550\u2550\u2550\u2550\u255d " +
            l +
            "\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u255d" +
            p +
            "\u255a\u2550\u255d  \u255a\u2550\u2550\u2550\u255d" +
            l +
            "\u255a\u2550\u255d" +
            p +
            " \u255a\u2550\u2550\u2550\u2550\u2550\u255d " +
            l +
            " \u255a\u2550\u2550\u2550\u2550\u2550\u255d " +
            p +
            "\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u255d" +
            r
        );

        System.out.println();

        // Sub-header rule
        int boxW = 67;
        String rule = b + "\u2550".repeat(boxW) + r;
        System.out.println("  " + rule);
        System.out.println(centred(w + bo + "AI-ASSISTED TEST AUTOMATION" + r, boxW));
        System.out.println(
            centred(di + "Interactive CLI  \u2022  " + SystemDefaults.getBuildVersion() + r, boxW)
        );
        System.out.println("  " + rule);
        System.out.println();

        // Project / AI info rows
        String project = session.project();
        if (project != null) {
            System.out.println("  " + di + "Project:" + r + "  " + bo + p + project + r);
        } else {
            System.out.println(
                "  " + di + "Project:" + r + "  " + di + "(none \u2014 /project <name>)" + r
            );
        }
        if (provider != null) {
            System.out.println("  " + di + "AI:      " + provider.describe() + r);
        }
        System.out.println();
        System.out.println(
            "  " + di + "Type a request in plain English, or /help for commands." + r
        );
        System.out.println();
    }

    private static String centred(String content, int innerWidth) {
        int vis = Theme.visibleLength(content);
        int pad = Math.max(0, innerWidth - vis);
        return "  " + " ".repeat(pad / 2) + content;
    }
}

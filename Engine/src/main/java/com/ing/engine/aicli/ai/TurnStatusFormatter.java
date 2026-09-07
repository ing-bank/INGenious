package com.ing.engine.aicli.ai;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formats {@link TurnStats} into the GitHub-Copilot-style status line
 * ({@code 9:52 PM  8m 46s - Claude Opus 4.8 - 445.0 credits}), shared by the AI
 * CLI (which colorizes it) and the IDE assistant (which shows it plain in the
 * footer).
 */
public final class TurnStatusFormatter {

    private TurnStatusFormatter() {}

    /** {@code 8m 46s} / {@code 46s} / {@code 1h 2m 3s}. */
    public static String formatDuration(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append("h ");
        }
        if (h > 0 || m > 0) {
            sb.append(m).append("m ");
        }
        sb.append(s).append("s");
        return sb.toString();
    }

    /** {@code claude-opus-4.8 -> Claude Opus 4.8}, {@code gpt-5.3-codex -> GPT 5.3 Codex}. */
    public static String displayModel(String id) {
        if (id == null || id.isBlank()) {
            return "model";
        }
        String[] parts = id.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            if (lower.equals("gpt")) {
                sb.append("GPT");
            } else if (lower.equals("ai")) {
                sb.append("AI");
            } else if (Character.isDigit(part.charAt(0))) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    /** Plain (no ANSI) status line: {@code 9:52 PM  8m 46s - Claude Opus 4.8 - 445.0 credits}. */
    public static String plainLine(TurnStats st, String fallbackModel) {
        String time = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
        String elapsed = formatDuration(st.elapsedMillis);
        String modelName = displayModel(st.model != null ? st.model : fallbackModel);
        String credits = String.format(Locale.US, "%.1f", st.credits);
        return (
            time + "  " + elapsed + "  \u2022  " + modelName + "  \u2022  " + credits + " credits"
        );
    }
}

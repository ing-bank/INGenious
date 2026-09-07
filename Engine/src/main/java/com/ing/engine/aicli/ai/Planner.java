package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.engine.aicli.planning.Plan;
import com.ing.engine.aicli.planning.PlanValidator;
import com.ing.engine.aicli.tools.ToolRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a natural-language request into either a validated {@link Plan}
 * (tool composition) or a plain answer (explanations). The LLM output is
 * hard-gated by {@link PlanValidator}; invalid plans get one repair round
 * before failing deterministically.
 */
public final class Planner {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolRegistry registry;

    public Planner(ToolRegistry registry) {
        this.registry = registry;
    }

    /** Either {@code plan} or {@code answer} is non-null, never both. */
    public static final class Outcome {
        private final Plan plan;
        private final String answer;

        public Outcome(Plan plan, String answer) {
            this.plan = plan;
            this.answer = answer;
        }

        public Plan plan() {
            return plan;
        }

        public String answer() {
            return answer;
        }
    }

    public Outcome plan(
        String request,
        String sessionSummary,
        List<ChatMessage> historyTail,
        AiProvider provider
    )
        throws AiProvider.AiException {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt(sessionSummary)));
        messages.addAll(historyTail);
        messages.add(ChatMessage.user(request));

        String content = provider.chat(messages);
        Outcome outcome = parse(content);
        if (outcome.answer() != null) return outcome;

        List<String> errors = PlanValidator.validate(outcome.plan(), registry);
        if (errors.isEmpty()) return outcome;

        // one repair round
        messages.add(ChatMessage.assistant(content));
        messages.add(
            ChatMessage.user(
                "Your plan is invalid:\n- " +
                String.join("\n- ", errors) +
                "\nRespond again with corrected JSON only."
            )
        );
        String repaired = provider.chat(messages);
        Outcome second = parse(repaired);
        if (second.answer() != null) return second;
        List<String> secondErrors = PlanValidator.validate(second.plan(), registry);
        if (secondErrors.isEmpty()) return second;
        throw new AiProvider.AiException(
            "The AI produced an invalid plan twice: " + String.join("; ", secondErrors)
        );
    }

    private Outcome parse(String content) throws AiProvider.AiException {
        String jsonText = extractJson(content);
        if (jsonText == null) {
            // no JSON at all → treat the whole message as an answer
            return new Outcome(null, content.trim());
        }
        try {
            JsonNode node = mapper.readTree(jsonText);
            String type = node.path("type").asText("plan");
            if ("answer".equals(type)) {
                return new Outcome(null, node.path("text").asText(""));
            }
            return new Outcome(Plan.fromJson(node, mapper), null);
        } catch (Exception e) {
            throw new AiProvider.AiException(
                "Could not parse the AI response as JSON: " + e.getMessage()
            );
        }
    }

    /** Extract the first top-level JSON object, tolerating markdown fences. */
    static String extractJson(String content) {
        if (content == null) return null;
        String s = content.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = s.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') depth++; else if (c == '}') {
                    depth--;
                    if (depth == 0) return s.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String systemPrompt(String sessionSummary) {
        return (
            "You are the planning engine of the INGenious test-automation CLI. " +
            "INGenious organizes work as: Projects contain Scenarios (folders) containing " +
            "Test Cases (YAML step lists). Steps reference Actions, Object Repository pages/objects, " +
            "and data sheets. Test Sets group test cases for execution.\n\n" +
            com.ing.engine.mcp.ConventionCatalog.condensedInstructions() +
            "\n\n" +
            "Session context:\n" +
            sessionSummary +
            "\n\n" +
            "Available tools (name(args; * = required, ? = optional): description):\n" +
            registry.promptCatalog() +
            "\nRespond with EXACTLY ONE minified JSON object and nothing else.\n" +
            "To act, respond: {\"type\":\"plan\",\"goal\":\"...\",\"steps\":[{\"id\":\"s1\",\"tool\":\"<tool id>\"," +
            "\"args\":{...},\"dependsOn\":[\"s0\"]}]}\n" +
            "To answer a question without acting, respond: {\"type\":\"answer\",\"text\":\"...\"}\n" +
            "Rules:\n" +
            "- Only use tools from the list above; never invent tools or arguments.\n" +
            "- Use ${sN.out.<field>} inside string args to reference a previous step's result field. " +
            "Reference ONLY fields the tool actually returns — never invent a field name. " +
            "action_info returns the action under 'name' (use ${sN.out.name}); " +
            "action_search and action_list return an ARRAY of {name,category,objectType,...}, " +
            "so reference the first match as ${sN.out.0.name}. There is no 'action' output field.\n" +
            "- When you already know the exact action name, pass it LITERALLY as a plain string " +
            "(e.g. \"action\":\"Set\") instead of a ${...} reference. Only use references for values " +
            "you cannot know at plan time. Never leave an unresolved ${...} placeholder as a step's action.\n" +
            "- When discovering an action for a step, pass action_search a 'category' matching the " +
            "step's object type (Browser, API, Mobile, Database, Kafka, General) so you get the right " +
            "action — e.g. for a Webservice/API step: action_search {\"query\":\"assert response contains\"," +
            "\"category\":\"API\"}. INGenious names checks with 'Assert' (not validate/verify); " +
            "search is synonym-aware but the category keeps results on the correct object type.\n" +
            "- Do not include a 'project' argument; the CLI injects the active project automatically.\n" +
            "- Only attach an 'input' to a step when the action actually takes one. Actions whose " +
            "metadata says input=NO (e.g. getRestRequest) must have NO input. For a REST call, use " +
            "THREE steps: setEndPoint (input = the URL), then getRestRequest (NO input), then an " +
            "assert action (input = the expected value). Never put the URL as input on getRestRequest.\n" +
            "- Prefer the smallest plan that satisfies the request.\n" +
            "- For new browser tests: discover objects first (browser_* tools + object_import_page into the Web OR), " +
            "author reusable steps, then compose and validate the test case.\n" +
            "- Ask for clarification via {\"type\":\"answer\"} only when a required argument is truly unknown."
        );
    }
}

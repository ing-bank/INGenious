package com.ing.ide.main.mainui.components.aichat.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.aichat.model.Tool;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * In-process INGenious tool server. Exposes Datalib operations as
 * OpenAI/MCP-style tools with typed JSON schemas and <strong>server-side
 * validation that runs before any mutation</strong> (valid action keyword,
 * existing scenario/test case, existing Object Repository element). The model
 * cannot bypass this gate.
 *
 * <p>The layer is transport-agnostic: {@link #execute(String, JsonNode)} takes a
 * tool name plus parsed arguments and returns a {@link ToolResult}, so the same
 * server can later be exposed over stdio/MCP to external clients.</p>
 */
public class INGeniousToolServer {
    private static final Logger LOG = Logger.getLogger(INGeniousToolServer.class.getName());

    private final AppMainFrame mainFrame;
    private final ActionCatalog actionCatalog = new ActionCatalog();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Set<String> READ_ONLY_TOOLS = new HashSet<>(
        Arrays.asList("listScenarios", "listTestCases", "readTestCase", "listActions")
    );

    public INGeniousToolServer(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /** Returns {@code true} if the tool only reads and may run without approval. */
    public boolean isReadOnly(String toolName) {
        return READ_ONLY_TOOLS.contains(toolName);
    }

    public boolean isKnownTool(String toolName) {
        if (toolName == null) {
            return false;
        }
        switch (toolName) {
            case "listScenarios":
            case "listTestCases":
            case "readTestCase":
            case "listActions":
            case "createScenario":
            case "addTestCase":
            case "addStep":
            case "createORObject":
                return true;
            default:
                return false;
        }
    }

    // ── Tool definitions (schemas) ─────────────────────────────────────────

    /** Builds the tool definitions advertised to the model. */
    public List<Tool> toolDefinitions() {
        List<Tool> tools = new ArrayList<>();
        tools.add(
            tool(
                "listScenarios",
                "List scenario names in the project. Set reusable=true for Reusable Components.",
                obj(
                    prop("reusable", "boolean", "List reusable scenarios instead of the test plan.")
                )
            )
        );
        tools.add(
            tool(
                "listTestCases",
                "List the test cases inside a scenario.",
                required(
                    obj(
                        prop("scenario", "string", "Scenario name."),
                        prop("reusable", "boolean", "Whether the scenario is a reusable component.")
                    ),
                    "scenario"
                )
            )
        );
        tools.add(
            tool(
                "readTestCase",
                "Read the steps of a test case.",
                required(
                    obj(
                        prop("scenario", "string", "Scenario name."),
                        prop("testCase", "string", "Test case name."),
                        prop("reusable", "boolean", "Whether the scenario is a reusable component.")
                    ),
                    "scenario",
                    "testCase"
                )
            )
        );
        tools.add(
            tool(
                "listActions",
                "List valid INGenious action keywords (use these exact keywords in addStep).",
                obj()
            )
        );
        tools.add(
            tool(
                "createScenario",
                "Create a new scenario in the test plan (or reusable components).",
                required(
                    obj(
                        prop("name", "string", "New scenario name."),
                        prop(
                            "reusable",
                            "boolean",
                            "Create under Reusable Components instead of the test plan."
                        )
                    ),
                    "name"
                )
            )
        );
        tools.add(
            tool(
                "addTestCase",
                "Add a test case to an existing scenario.",
                required(
                    obj(
                        prop("scenario", "string", "Existing scenario name."),
                        prop("testCase", "string", "New test case name."),
                        prop("reusable", "boolean", "Whether the scenario is a reusable component.")
                    ),
                    "scenario",
                    "testCase"
                )
            )
        );
        tools.add(
            tool(
                "addStep",
                "Append a step to a test case. The action must be a valid INGenious keyword and any " +
                "referenced Object Repository element must already exist.",
                required(
                    obj(
                        prop("scenario", "string", "Scenario name."),
                        prop("testCase", "string", "Test case name."),
                        prop(
                            "action",
                            "string",
                            "Valid INGenious action keyword (e.g. click, navigate, asserttitle)."
                        ),
                        prop(
                            "object",
                            "string",
                            "Object Repository element name, if the action targets an element."
                        ),
                        prop("input", "string", "Input data for the action, if required."),
                        prop(
                            "reference",
                            "string",
                            "Object Repository page name that contains the object."
                        ),
                        prop("condition", "string", "Optional run condition."),
                        prop("description", "string", "Optional human-readable description."),
                        prop("reusable", "boolean", "Whether the scenario is a reusable component.")
                    ),
                    "scenario",
                    "testCase",
                    "action"
                )
            )
        );
        tools.add(
            tool(
                "createORObject",
                "Create an Object Repository web element (and its page if needed) with one locator.",
                required(
                    obj(
                        prop("page", "string", "Object Repository page name."),
                        prop("element", "string", "Element (object) name."),
                        prop(
                            "locatorType",
                            "string",
                            "Locator type: xpath or css (or another supported attribute)."
                        ),
                        prop("locatorValue", "string", "Locator value.")
                    ),
                    "page",
                    "element",
                    "locatorType",
                    "locatorValue"
                )
            )
        );
        return tools;
    }

    // ── Execution ──────────────────────────────────────────────────────────

    /**
     * Executes a tool call. Mutating operations are marshalled onto the EDT to
     * stay thread-safe with the Swing model and table events.
     */
    public ToolResult execute(String toolName, JsonNode args) {
        try {
            if (isReadOnly(toolName)) {
                return executeInternal(toolName, args);
            }
            return runOnEdt(() -> executeInternal(toolName, args));
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Tool execution failed: " + toolName, ex);
            return ToolResult.error("Tool '" + toolName + "' failed: " + ex.getMessage());
        }
    }

    private ToolResult executeInternal(String toolName, JsonNode args) {
        Project project = mainFrame.getProject();
        if (project == null) {
            return ToolResult.error("No project is open. Ask the user to open a project first.");
        }
        switch (toolName) {
            case "listScenarios":
                return listScenarios(project, args);
            case "listTestCases":
                return listTestCases(project, args);
            case "readTestCase":
                return readTestCase(project, args);
            case "listActions":
                return listActions();
            case "createScenario":
                return createScenario(project, args);
            case "addTestCase":
                return addTestCase(project, args);
            case "addStep":
                return addStep(project, args);
            case "createORObject":
                return createORObject(project, args);
            default:
                return ToolResult.error("Unknown tool: " + toolName);
        }
    }

    // ── Read-only tools ────────────────────────────────────────────────────

    private ToolResult listScenarios(Project project, JsonNode args) {
        boolean reusable = args.path("reusable").asBoolean(false);
        List<Scenario> scenarios = reusable
            ? project.getReusableScenarios()
            : project.getScenarios();
        ArrayNode arr = mapper.createArrayNode();
        for (Scenario s : scenarios) {
            arr.add(s.getName());
        }
        ObjectNode result = mapper.createObjectNode();
        result.put("reusable", reusable);
        result.set("scenarios", arr);
        return ToolResult.ok(result.toString());
    }

    private ToolResult listTestCases(Project project, JsonNode args) {
        String scenarioName = args.path("scenario").asText("");
        boolean reusable = args.path("reusable").asBoolean(false);
        Scenario scenario = findScenario(project, scenarioName, reusable);
        if (scenario == null) {
            return ToolResult.error("Scenario not found: " + scenarioName);
        }
        ArrayNode arr = mapper.createArrayNode();
        for (TestCase tc : scenario.getTestCases()) {
            arr.add(tc.getName());
        }
        ObjectNode result = mapper.createObjectNode();
        result.put("scenario", scenario.getName());
        result.set("testCases", arr);
        return ToolResult.ok(result.toString());
    }

    private ToolResult readTestCase(Project project, JsonNode args) {
        String scenarioName = args.path("scenario").asText("");
        String testCaseName = args.path("testCase").asText("");
        boolean reusable = args.path("reusable").asBoolean(false);
        Scenario scenario = findScenario(project, scenarioName, reusable);
        if (scenario == null) {
            return ToolResult.error("Scenario not found: " + scenarioName);
        }
        TestCase tc = scenario.getTestCaseByName(testCaseName);
        if (tc == null) {
            return ToolResult.error("Test case not found: " + testCaseName);
        }
        synchronized (tc) {
            tc.loadTestCaseTableModel();
        }
        ArrayNode steps = mapper.createArrayNode();
        for (TestStep step : tc.getTestSteps()) {
            ObjectNode row = mapper.createObjectNode();
            row.put("object", step.getObject());
            row.put("action", step.getAction());
            row.put("input", step.getInput());
            row.put("condition", step.getCondition());
            row.put("reference", step.getReference());
            steps.add(row);
        }
        ObjectNode result = mapper.createObjectNode();
        result.put("scenario", scenario.getName());
        result.put("testCase", tc.getName());
        result.set("steps", steps);
        return ToolResult.ok(result.toString());
    }

    private ToolResult listActions() {
        if (!actionCatalog.isLoaded()) {
            return ToolResult.ok("Action catalog unavailable; refer to the authoring skill.");
        }
        ArrayNode arr = mapper.createArrayNode();
        for (String a : actionCatalog.all()) {
            arr.add(a);
        }
        ObjectNode result = mapper.createObjectNode();
        result.set("actions", arr);
        return ToolResult.ok(result.toString());
    }

    // ── Mutating tools (validated) ─────────────────────────────────────────

    private ToolResult createScenario(Project project, JsonNode args) {
        String name = args.path("name").asText("").trim();
        boolean reusable = args.path("reusable").asBoolean(false);
        if (name.isEmpty()) {
            return ToolResult.error("Scenario name is required.");
        }
        Scenario existing = findScenario(project, name, reusable);
        if (existing != null) {
            return ToolResult.error("A scenario named '" + name + "' already exists.");
        }
        Scenario created = reusable ? project.addReusableScenario(name) : project.addScenario(name);
        if (created == null) {
            return ToolResult.error("Could not create scenario '" + name + "'.");
        }
        project.save();
        return ToolResult.ok("Created scenario '" + name + "'" + (reusable ? " (reusable)." : "."));
    }

    private ToolResult addTestCase(Project project, JsonNode args) {
        String scenarioName = args.path("scenario").asText("").trim();
        String testCaseName = args.path("testCase").asText("").trim();
        boolean reusable = args.path("reusable").asBoolean(false);
        if (testCaseName.isEmpty()) {
            return ToolResult.error("Test case name is required.");
        }
        Scenario scenario = findScenario(project, scenarioName, reusable);
        if (scenario == null) {
            return ToolResult.error("Scenario not found: " + scenarioName);
        }
        if (scenario.getTestCaseByName(testCaseName) != null) {
            return ToolResult.error(
                "Test case '" + testCaseName + "' already exists in '" + scenarioName + "'."
            );
        }
        TestCase tc = scenario.addTestCase(testCaseName);
        if (tc == null) {
            return ToolResult.error("Could not create test case '" + testCaseName + "'.");
        }
        scenario.save();
        return ToolResult.ok(
            "Created test case '" + testCaseName + "' in scenario '" + scenarioName + "'."
        );
    }

    private ToolResult addStep(Project project, JsonNode args) {
        String scenarioName = args.path("scenario").asText("").trim();
        String testCaseName = args.path("testCase").asText("").trim();
        String action = args.path("action").asText("").trim();
        String object = args.path("object").asText("").trim();
        String input = args.path("input").asText("");
        String reference = args.path("reference").asText("").trim();
        String condition = args.path("condition").asText("");
        String description = args.path("description").asText("");
        boolean reusable = args.path("reusable").asBoolean(false);

        // Validation gate — runs before any mutation.
        if (action.isEmpty()) {
            return ToolResult.error("Action is required.");
        }
        if (!actionCatalog.isValid(action)) {
            return ToolResult.error(
                "Invalid action keyword: '" + action + "'. Call listActions to see valid keywords."
            );
        }
        Scenario scenario = findScenario(project, scenarioName, reusable);
        if (scenario == null) {
            return ToolResult.error("Scenario not found: " + scenarioName);
        }
        TestCase tc = scenario.getTestCaseByName(testCaseName);
        if (tc == null) {
            return ToolResult.error("Test case not found: " + testCaseName);
        }
        if (!object.isEmpty() && !objectExists(project, reference, object)) {
            return ToolResult.error(
                "Object Repository element not found: '" +
                (reference.isEmpty() ? object : reference + "." + object) +
                "'. Create it with createORObject first."
            );
        }

        synchronized (tc) {
            tc.loadTestCaseTableModel();
            TestStep step = tc.addNewStep();
            step
                .setAction(action)
                .setObject(object)
                .setInput(input)
                .setReference(reference)
                .setCondition(condition)
                .setDescription(description);
            tc.save();
        }
        return ToolResult.ok(
            "Added step '" +
            action +
            "'" +
            (object.isEmpty() ? "" : " on '" + object + "'") +
            " to '" +
            scenarioName +
            " / " +
            testCaseName +
            "'."
        );
    }

    private ToolResult createORObject(Project project, JsonNode args) {
        String pageName = args.path("page").asText("").trim();
        String elementName = args.path("element").asText("").trim();
        String locatorType = args.path("locatorType").asText("").trim();
        String locatorValue = args.path("locatorValue").asText("");

        if (pageName.isEmpty() || elementName.isEmpty()) {
            return ToolResult.error("Both page and element names are required.");
        }
        if (locatorType.isEmpty() || locatorValue.isEmpty()) {
            return ToolResult.error("locatorType and locatorValue are required.");
        }
        WebOR webOR = project.getObjectRepository().getWebOR();
        WebORPage page = webOR.getPageByName(pageName);
        if (page == null) {
            page = webOR.addPage(pageName);
            if (page == null) {
                return ToolResult.error("Could not create page '" + pageName + "'.");
            }
        }
        if (findObjectOnPage(page, elementName) != null) {
            return ToolResult.error(
                "Element '" + elementName + "' already exists on page '" + pageName + "'."
            );
        }
        WebORObject object = page.addObject(elementName);
        if (object == null) {
            return ToolResult.error("Could not create element '" + elementName + "'.");
        }
        boolean applied = false;
        for (ORAttribute attr : object.getAttributes()) {
            if (attr.getName() != null && attr.getName().equalsIgnoreCase(locatorType)) {
                attr.setValue(locatorValue);
                attr.setPreference("1");
                applied = true;
                break;
            }
        }
        if (!applied) {
            return ToolResult.error(
                "Unsupported locatorType '" +
                locatorType +
                "'. Use one of the element's locator attributes (e.g. xpath, css)."
            );
        }
        project.getObjectRepository().save();
        return ToolResult.ok(
            "Created element '" +
            pageName +
            "." +
            elementName +
            "' with " +
            locatorType +
            " locator."
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Scenario findScenario(Project project, String name, boolean reusable) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Scenario scenario = reusable
            ? project.getReusableScenarioByName(name)
            : project.getTestPlanScenarioByName(name);
        // Fall back to a broad lookup if the reusable flag was guessed wrong.
        return scenario != null ? scenario : project.getScenarioByName(name);
    }

    private boolean objectExists(Project project, String pageName, String objectName) {
        WebOR webOR = project.getObjectRepository().getWebOR();
        if (pageName != null && !pageName.isEmpty()) {
            WebORPage page = webOR.getPageByName(pageName);
            return page != null && findObjectOnPage(page, objectName) != null;
        }
        for (WebORPage page : webOR.getPages()) {
            if (findObjectOnPage(page, objectName) != null) {
                return true;
            }
        }
        return false;
    }

    private WebORObject findObjectOnPage(WebORPage page, String objectName) {
        for (ObjectGroup<WebORObject> group : page.getObjectGroups()) {
            WebORObject obj = group.getObjectByName(objectName);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    private ToolResult runOnEdt(java.util.function.Supplier<ToolResult> action) {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.get();
        }
        AtomicReference<ToolResult> ref = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(action.get()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ToolResult.error("Operation interrupted.");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            return ToolResult.error("Operation failed: " + cause.getMessage());
        }
        return ref.get();
    }

    // ── Schema builders ────────────────────────────────────────────────────

    private Tool tool(String name, String description, ObjectNode parameters) {
        return new Tool(name, description, parameters);
    }

    private ObjectNode obj(ObjectNode... props) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = mapper.createObjectNode();
        for (ObjectNode p : props) {
            properties.setAll((ObjectNode) p);
        }
        schema.set("properties", properties);
        return schema;
    }

    private ObjectNode required(ObjectNode schema, String... requiredFields) {
        ArrayNode req = mapper.createArrayNode();
        for (String r : requiredFields) {
            req.add(r);
        }
        schema.set("required", req);
        return schema;
    }

    private ObjectNode prop(String name, String type, String description) {
        ObjectNode wrapper = mapper.createObjectNode();
        ObjectNode body = mapper.createObjectNode();
        body.put("type", type);
        body.put("description", description);
        wrapper.set(name, body);
        return wrapper;
    }
}

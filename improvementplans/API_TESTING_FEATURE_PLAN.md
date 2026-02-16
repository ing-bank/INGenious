# API Testing Feature - Design Plan

**Date:** February 16, 2026  
**Feature Name:** API Workbench / API Tester  
**Target Location:** New main tab alongside TestDesign, TestExecution, Dashboard  
**Estimated Effort:** 2-3 weeks for MVP, additional 2-3 weeks for advanced features  

---

## Executive Summary

Create a Postman-like API testing experience within INGenious, allowing users to:
- Construct and send HTTP requests (GET, POST, PUT, PATCH, DELETE)
- View and validate responses with syntax highlighting
- Organize requests into collections
- Chain requests with variable substitution
- Save and reuse requests across projects

---

## Architecture Overview

### Integration Strategy
```
IDE Module
├── main/mainui/components/
│   └── apitester/              ← NEW PACKAGE
│       ├── APITester.java           (Controller)
│       ├── APITesterUI.java         (Main Panel - Swing)
│       │
│       ├── request/
│       │   ├── RequestPanel.java        (Request builder)
│       │   ├── HeadersPanel.java        (Key-value headers editor)
│       │   ├── ParamsPanel.java         (Query/path params)
│       │   ├── BodyPanel.java           (JSON/XML/Form body editor)
│       │   └── AuthPanel.java           (Authentication configs)
│       │
│       ├── response/
│       │   ├── ResponsePanel.java       (Response viewer)
│       │   ├── ResponseBodyViewer.java  (Syntax highlighted body)
│       │   ├── ResponseHeadersView.java (Response headers table)
│       │   └── ResponseValidator.java   (Assertions UI)
│       │
│       ├── collections/
│       │   ├── CollectionTree.java      (Left-side tree of saved requests)
│       │   ├── CollectionManager.java   (CRUD for collections)
│       │   └── RequestNode.java         (Tree node model)
│       │
│       └── util/
│           ├── APIHttpClient.java       (Wrapper around java.net.http.HttpClient)
│           ├── VariableResolver.java    (Environment/collection variables)
│           └── RequestHistoryManager.java (Recent requests)
│
├── main/fx/api/                  ← OPTIONAL JavaFX components
│   ├── FXResponseBodyViewer.java    (CodeMirror-style syntax viewer)
│   └── FXJSONTreeView.java          (Collapsible JSON tree)
```

### Data Model (Datalib module)
```
Datalib
└── src/main/java/com/ing/datalib/api/
    ├── APICollection.java       (Collection of requests)
    ├── APIRequest.java          (Single request definition)
    ├── APIResponse.java         (Response capture)
    ├── APIEnvironment.java      (Environment variables)
    └── APIAssertion.java        (Validation rules)
```

### Storage Format
```
Project/
└── api/
    ├── collections/
    │   └── my-collection.json
    ├── environments/
    │   ├── dev.json
    │   ├── staging.json
    │   └── prod.json
    └── history/
        └── recent-requests.json
```

---

## UI Design

### Main Layout (Postman-inspired)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  [Collections ▼]  [Environments ▼]  [History ⏱]              [🌙 Theme]     │
├──────────────────┬──────────────────────────────────────────────────────────┤
│                  │ ┌─────────────────────────────────────────────────────┐  │
│  📁 Collections  │ │ [GET ▼]  https://api.example.com/users/{{userId}}   │  │
│  ├─ 📂 Users API │ │                                          [▶ Send]  │  │
│  │  ├─ GET Users │ └─────────────────────────────────────────────────────┘  │
│  │  ├─ POST User │                                                          │
│  │  └─ PUT User  │ ┌─────────────────────────────────────────────────────┐  │
│  └─ 📂 Auth API  │ │  [Params] [Headers] [Auth] [Body] [Pre-req] [Tests] │  │
│     └─ POST Login│ ├─────────────────────────────────────────────────────┤  │
│                  │ │  Query Parameters:                                   │  │
│  📁 History      │ │  ┌──────────┬────────────┬──┐                       │  │
│  ├─ GET /users   │ │  │ Key      │ Value      │✓ │                       │  │
│  └─ POST /login  │ │  ├──────────┼────────────┼──┤                       │  │
│                  │ │  │ page     │ 1          │☑ │                       │  │
│                  │ │  │ limit    │ 20         │☑ │                       │  │
├──────────────────┤ │  └──────────┴────────────┴──┘                       │  │
│  Variables       │ └─────────────────────────────────────────────────────┘  │
│  ┌────────────┐  │                                                          │
│  │baseUrl:... │  │ ═══════════════════════════════════════════════════════  │
│  │userId: 123 │  │                                                          │
│  └────────────┘  │ ┌─────────────────────────────────────────────────────┐  │
│                  │ │  Response  [200 OK]  [245 ms]  [1.2 KB]             │  │
│                  │ ├─────────────────────────────────────────────────────┤  │
│                  │ │  [Body ▼] [Headers] [Cookies] [Test Results]        │  │
│                  │ ├─────────────────────────────────────────────────────┤  │
│                  │ │  {                                                   │  │
│                  │ │    "id": 123,                                        │  │
│                  │ │    "name": "John Doe",                               │  │
│                  │ │    "email": "john@example.com"                       │  │
│                  │ │  }                                                   │  │
│                  │ └─────────────────────────────────────────────────────┘  │
└──────────────────┴──────────────────────────────────────────────────────────┘
```

### Key UI Components

#### 1. Request Builder Panel
- **Method Selector**: Dropdown (GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD)
- **URL Bar**: Text field with syntax highlighting for variables `{{varName}}`
- **Send Button**: Primary action button with loading state
- **Save Button**: Save to collection

#### 2. Request Tab Panel
| Tab | Contents |
|-----|----------|
| **Params** | Key-value table for query parameters |
| **Headers** | Key-value table with autocomplete for common headers |
| **Auth** | None / Basic Auth / Bearer Token / API Key / OAuth 2.0 |
| **Body** | None / Raw (JSON/XML/Text) / Form Data / x-www-form-urlencoded / Binary |
| **Pre-request** | JavaScript/Groovy script editor for pre-processing |
| **Tests** | Assertions editor (status code, JSON path, headers) |

#### 3. Response Viewer Panel
- **Status Line**: Status code (color-coded), response time, response size
- **Body Tab**: 
  - Pretty (formatted JSON/XML with syntax highlighting)
  - Raw (unformatted text)
  - Preview (HTML rendering for HTML responses)
- **Headers Tab**: Response headers table
- **Cookies Tab**: Parsed cookies
- **Test Results Tab**: Pass/fail assertions

#### 4. Collections Tree (Left Panel)
- Hierarchical tree view of saved requests
- Drag-and-drop reordering
- Right-click context menu: Rename, Duplicate, Delete, Export
- Search/filter bar

---

## Data Models

### APIRequest.java
```java
public class APIRequest {
    private String id;              // UUID
    private String name;
    private String description;
    private HttpMethod method;      // GET, POST, PUT, PATCH, DELETE
    private String url;             // Can contain {{variables}}
    private List<KeyValuePair> queryParams;
    private List<KeyValuePair> headers;
    private RequestBody body;
    private AuthConfig auth;
    private String preRequestScript;
    private List<APIAssertion> assertions;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### APICollection.java
```java
public class APICollection {
    private String id;
    private String name;
    private String description;
    private List<APIRequest> requests;
    private List<APICollection> folders;  // Nested folders
    private List<KeyValuePair> variables; // Collection-level variables
    private AuthConfig defaultAuth;       // Inherited by requests
}
```

### APIEnvironment.java
```java
public class APIEnvironment {
    private String id;
    private String name;
    private Map<String, String> variables;
    private boolean isActive;
}
```

### APIAssertion.java
```java
public class APIAssertion {
    private AssertionType type;     // STATUS_CODE, JSON_PATH, HEADER, RESPONSE_TIME
    private String target;          // e.g., "$.user.id" or "Content-Type"
    private String operator;        // equals, contains, exists, lessThan, etc.
    private String expectedValue;
}
```

---

## Technical Implementation

### Phase 1: Core Infrastructure (Week 1)

#### 1.1 Create Package Structure
```
IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/
Datalib/src/main/java/com/ing/datalib/api/
```

#### 1.2 Implement APIHttpClient.java
Wrapper around `java.net.http.HttpClient` with:
- Connection timeout configuration
- SSL/TLS handling (including self-signed cert option)
- Proxy support
- Response timing
- Cookie management

```java
public class APIHttpClient {
    private final HttpClient httpClient;
    
    public APIResponse send(APIRequest request) {
        Instant start = Instant.now();
        HttpRequest httpRequest = buildRequest(request);
        HttpResponse<String> response = httpClient.send(httpRequest, 
            HttpResponse.BodyHandlers.ofString());
        Duration duration = Duration.between(start, Instant.now());
        
        return new APIResponse(
            response.statusCode(),
            response.body(),
            response.headers(),
            duration
        );
    }
}
```

#### 1.3 Implement Data Models
- `APIRequest`, `APICollection`, `APIEnvironment`, `APIAssertion`
- JSON serialization/deserialization using Jackson

#### 1.4 Implement Storage Layer
- Save/load collections to `Project/api/collections/`
- Save/load environments to `Project/api/environments/`
- Recently used requests in `Project/api/history/`

### Phase 2: Basic UI (Week 2)

#### 2.1 Create Main Panel (`APITesterUI.java`)
- `JSplitPane` with collections tree on left, request builder on right
- Integrate with `SlideShow` in `AppMainFrame`

#### 2.2 Implement Request Builder
- URL bar with method dropdown
- Tabbed pane for Params, Headers, Body
- Send button with SwingWorker for async execution

#### 2.3 Implement Response Viewer
- Status display
- Syntax-highlighted body viewer (using RSyntaxTextArea or embedded JavaFX)
- Headers table

#### 2.4 Wire Up Menu/Toolbar
- Add "API Tester" menu item
- Add toolbar button with API icon

### Phase 3: Collections & Environment (Week 3)

#### 3.1 Collections Tree
- JTree with custom renderer
- Drag-and-drop support
- Right-click context menu

#### 3.2 Environment Manager
- Environment dropdown in toolbar
- Variable resolution in URLs and bodies

#### 3.3 Request History
- Auto-save last N requests
- Quick access sidebar

### Phase 4: Advanced Features (Week 4+)

#### 4.1 Assertions & Test Results
- Visual assertion builder
- Test results panel with pass/fail indicators
- Export test results

#### 4.2 Authentication
- Basic Auth
- Bearer Token
- API Key (header/query)
- OAuth 2.0 flows

#### 4.3 Import/Export
- Import from Postman collection (v2.1)
- Import from OpenAPI/Swagger spec
- Export to Postman format
- Export to cURL command

#### 4.4 Code Generation
- Generate test scripts for INGenious test cases
- Generate cURL commands
- Generate code snippets (Java, Python, Node.js)

---

## Integration Points

### Menu Bar Integration (FXMenuBar.java / AppMenuBar.java)
```java
// Add to Tools menu
Menu toolsMenu = new Menu("Tools");
MenuItem apiTester = new MenuItem("API Workbench");
apiTester.setOnAction(e -> showAPITester());
toolsMenu.getItems().add(apiTester);

// Or add as main navigation item
MenuItem apiNav = new MenuItem("API Tester");
```

### SlideShow Integration (AppMainFrame.java)
```java
// In initSlideShow() or similar
APITester apiTester = new APITester(this);
slideShow.addSlide("APITester", apiTester.getAPITesterUI());

public void showAPITester() {
    getGlassPane().setVisible(false);
    slideShow.showSlide("APITester");
    if (fxStatusBar != null) {
        fxStatusBar.setCurrentView("API Tester");
    }
}
```

### Test Case Integration
- Option to record API calls from API Tester directly into test cases
- Use existing `Webservice.java` command framework for execution
- Transform saved requests to test steps

---

## Dependencies

### Required (already in project)
- `com.fasterxml.jackson.core:jackson-databind` - JSON serialization
- `com.jayway.jsonpath:json-path` - JSON path assertions
- `java.net.http.HttpClient` - HTTP client (Java 11+)

### New Dependencies
```xml
<!-- Syntax highlighting for response body -->
<dependency>
    <groupId>com.fifesoft</groupId>
    <artifactId>rsyntaxtextarea</artifactId>
    <version>3.5.2</version>
</dependency>

<!-- Code editor with autocomplete -->
<dependency>
    <groupId>com.fifesoft</groupId>
    <artifactId>autocomplete</artifactId>
    <version>3.5.2</version>
</dependency>

<!-- YAML parsing for OpenAPI import (optional) -->
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.2</version>
</dependency>
```

---

## File Structure After Implementation

```
IDE/src/main/java/com/ing/ide/main/
├── mainui/
│   ├── components/
│   │   ├── apitester/
│   │   │   ├── APITester.java
│   │   │   ├── APITesterUI.java
│   │   │   ├── request/
│   │   │   │   ├── RequestPanel.java
│   │   │   │   ├── RequestTabPane.java
│   │   │   │   ├── HeadersPanel.java
│   │   │   │   ├── ParamsPanel.java
│   │   │   │   ├── BodyPanel.java
│   │   │   │   └── AuthPanel.java
│   │   │   ├── response/
│   │   │   │   ├── ResponsePanel.java
│   │   │   │   ├── ResponseBodyViewer.java
│   │   │   │   └── ResponseHeadersTable.java
│   │   │   ├── collections/
│   │   │   │   ├── CollectionTree.java
│   │   │   │   └── CollectionManager.java
│   │   │   └── util/
│   │   │       ├── APIHttpClient.java
│   │   │       ├── VariableResolver.java
│   │   │       └── SyntaxHighlighter.java
│   │   └── ... (existing)
│   └── ... (existing)

Datalib/src/main/java/com/ing/datalib/
├── api/
│   ├── APIRequest.java
│   ├── APIResponse.java
│   ├── APICollection.java
│   ├── APIEnvironment.java
│   ├── APIAssertion.java
│   ├── KeyValuePair.java
│   ├── RequestBody.java
│   └── AuthConfig.java
└── ... (existing)

Resources/Projects/{ProjectName}/
└── api/
    ├── collections/
    │   └── default-collection.json
    ├── environments/
    │   └── default-env.json
    └── history/
        └── recent.json
```

---

## UI Theme Consistency

Use existing INGenious theme system:
- `FlatLaf` for Swing components
- Custom colors via UIManager keys
- Icons from `INGIcons` (Material Design 2 + FontAwesome)

### Suggested Icons
| Element | Icon | Library |
|---------|------|---------|
| API Tester menu | `mdi2a-api` | Material Design |
| GET method | `mdi2a-arrow-down-bold` (green) | Material Design |
| POST method | `mdi2p-plus-thick` (yellow) | Material Design |
| PUT method | `mdi2p-pencil` (blue) | Material Design |
| DELETE method | `mdi2t-trash-can` (red) | Material Design |
| Collection folder | `mdi2f-folder` | Material Design |
| Request item | `mdi2f-file-document` | Material Design |
| Send button | `mdi2p-play` | Material Design |
| Save button | `mdi2c-content-save` | Material Design |

---

## MVP Features (Release 1.0)

- [x] Basic UI layout with split pane
- [x] Support for GET, POST, PUT, PATCH, DELETE
- [x] URL bar with variable substitution `{{var}}`
- [x] Headers editor (key-value pairs)
- [x] Query params editor
- [x] Body editor (raw JSON/XML/Text)
- [x] Response viewer with syntax highlighting
- [x] Response headers display
- [x] Status code, time, size display
- [x] Save/load requests to collections
- [x] Basic auth support
- [x] Request history (last 50)

## Future Features (Post-MVP)

- [ ] OAuth 2.0 authentication flows
- [ ] Pre-request scripts (JavaScript/Groovy)
- [ ] Test assertions with visual builder
- [ ] Import from Postman collections
- [ ] Import from OpenAPI/Swagger specs
- [ ] Export to cURL / code snippets
- [ ] Generate INGenious test steps from requests
- [ ] Request chaining with variable extraction
- [ ] WebSocket support
- [ ] GraphQL support
- [ ] Mock server

---

## Success Metrics

1. **Usability**: Users can send their first API request within 30 seconds
2. **Reliability**: All standard HTTP methods work correctly
3. **Performance**: Response displayed within 100ms of completion
4. **Integration**: Saved requests can be converted to test steps

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Complex SSL/TLS handling | Reuse patterns from `Webservice.java` |
| Large response bodies | Stream responses, virtualize display |
| UI performance with many requests | Lazy loading, pagination |
| Cross-platform compatibility | Use pure Java, avoid native code |

---

## Next Steps

1. **Approval**: Review and approve this design plan
2. **Setup**: Create package structure and stub classes
3. **Phase 1**: Implement HTTP client and data models (Week 1)
4. **Phase 2**: Build basic UI (Week 2)
5. **Phase 3**: Add collections and environments (Week 3)
6. **Testing**: Internal testing and refinement
7. **Release**: Document and release MVP

---

## Appendix: Similar Features in Other Tools

### Postman (Inspiration)
- Collections with folders
- Environments with variables
- Pre-request and test scripts
- Response visualization (JSON tree, etc.)

### Insomnia
- Simpler UI than Postman
- Plugin system
- GraphQL support built-in

### Thunder Client (VS Code)
- Lightweight, fast
- Inline test assertions
- Collection-based organization

The INGenious API Tester aims to combine the simplicity of Thunder Client with the power of Postman's collections and environments, while integrating seamlessly with the existing test automation framework.

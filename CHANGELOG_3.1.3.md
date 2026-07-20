# INGenious 3.1.3 — Release Notes

Generated: 2026-06-20

## Table of Contents

1. [API Workbench — Proxy Tab](#1-api-workbench--proxy-tab)
2. [API Workbench — Curl Paste Restricted Header Fix](#2-api-workbench--curl-paste-restricted-header-fix)
3. [Web Object Repository — `JSPath` Locator Attribute](#3-web-object-repository--jspath-locator-attribute)

---

## 1. API Workbench — Proxy Tab

### What changed

A new **Proxy** tab has been added to the API Workbench request panel, sitting alongside the existing Params, Headers, Body, Auth, and Settings tabs. Users can configure per-request HTTP proxy settings directly in the UI without touching any configuration files, and those proxy details are carried forward automatically when converting a request into an INGenious test case.

---

### Data model (`Datalib`)

**New class: `ProxyConfig`**

`com.ing.datalib.api.ProxyConfig` is a new serializable, Jackson-annotated model class with three fields:

| Field | Type | Default |
|---|---|---|
| `enabled` | `boolean` | `false` |
| `host` | `String` | `""` |
| `port` | `String` | `""` |

Key methods:
- `hasValidConfig()` — returns `true` when `enabled` is set and both `host` and `port` are non-blank.
- `copy()` — deep-copies the object (used by the existing request copy chain).

**Updated class: `APIRequest`**

- Added `private ProxyConfig proxyConfig` field after `certificateConfig`.
- Constructor initialises `proxyConfig` to a fresh `ProxyConfig()`.
- `getProxyConfig()` / `setProxyConfig(ProxyConfig)` added; the setter updates `updatedAt` in line with the rest of the setters.
- `copy()` includes a deep copy of `proxyConfig` via `proxyConfig.copy()`.

---

### UI (`IDE` — `RequestPanel`)

**New `ProxyPanel` class**

A new top-level inner class `ProxyPanel extends JPanel` was added at the end of `RequestPanel.java`. It follows the same structural pattern as `SettingsPanel`:

- **"Use Proxy" checkbox** — when unchecked (default), the Host and Port text fields are disabled and greyed out. When checked, they become editable.
- **Host field** (`JTextField`, 30 columns) with tooltip `"Proxy host (e.g., proxy.example.com or 127.0.0.1)"`.
- **Port field** (`JTextField`, 10 columns) with tooltip `"Proxy port (e.g., 8080)"`.
- **Informational note label** (`"When enabled, the request is routed through the configured proxy."`) rendered in italic at 11 pt in the secondary/disabled foreground color.
- `loadProxy(ProxyConfig)` — populates the UI from a `ProxyConfig` (null-safe).
- `updateRequest(APIRequest)` — writes the current UI values back to `request.getProxyConfig()`, creating a new `ProxyConfig` if one is not yet present.
- `refreshThemeColors()` — updates all child component colors from `UIManager` entries, consistent with the rest of the request panel's theme support.

**Wiring in `RequestPanel`**

- `private ProxyPanel proxyPanel` field added.
- `initComponents()`: `proxyPanel = new ProxyPanel()` created and `tabPane.addTab("Proxy", proxyPanel)` added after the Settings tab.
- `loadRequest(APIRequest)`: `proxyPanel.loadProxy(request.getProxyConfig())` called after settings are loaded.
- `updateRequest(APIRequest)`: `proxyPanel.updateRequest(request)` called after `settingsPanel.updateRequest(request)`.
- `refreshThemeColors()`: `proxyPanel.refreshThemeColors()` called alongside the existing settings panel refresh.

---

### Request execution (`IDE` — `APIHttpClient`)

`getHttpClient(APIRequest request)` was refactored from a certificate-only custom-client path into a unified builder that handles four combinations:

| Proxy configured? | Certs configured? | Client built |
|---|---|---|
| No | No | Shared `httpClient` / `insecureHttpClient` (unchanged) |
| Yes | No | Custom builder + `ProxySelector.of(InetSocketAddress)` |
| No | Yes | Custom builder + `SSLContext` (as before) |
| Yes | Yes | Custom builder + both `ProxySelector` and `SSLContext` |

The proxy is applied via:
```java
builder.proxy(
    ProxySelector.of(
        new InetSocketAddress(proxyConfig.getHost().trim(), Integer.parseInt(proxyConfig.getPort().trim()))
    )
);
```

A `buildTrustAllSSLContext()` helper was extracted from the old inline trust-all logic so it can be reused when only a proxy (and no certificate) is configured alongside the trust-all mode.

New imports added: `java.net.InetSocketAddress`, `java.net.ProxySelector`.

---

### Convert to Test flow (`IDE` — `RequestPanel` + `APITester`)

**`showConvertToTestDialog()` — proxy question**

When converting a request that has `ProxyConfig.hasValidConfig() == true`, a new dialog step is inserted before the test case is created:

> *"This request uses a proxy. Where would you like to save the proxy details?"*
>
> Options: **Default API Config** | **New API Config** | **Cancel**

- **Cancel** / close → the whole conversion is aborted.
- **Default API Config** → proxy details are saved into the existing `default` API config.
- **New API Config** → a second input dialog asks for an alias name; the conversion is aborted if the alias is blank or the dialog is cancelled.

After the user's choice, `apiTester.saveProxyToApiConfig(proxyConfig, alias)` is called. If it returns `false` (e.g. project not open), an error message is shown and the conversion stops.

The test case is then created via the new `convertRequestToTestCase(request, scenario, testCaseName, proxyConfigAlias)` overload.

**`APITester` — new overload: `convertRequestToTestCase(request, scenario, name, alias)`**

The existing no-alias signature now delegates to this 4-argument version with `null` as the alias. The new overload passes `alias` through to `buildStepsForRequest`.

**`APITester` — updated `buildStepsForRequest`**

A new overload `buildStepsForRequest(TestCase, APIRequest, String apiConfigAlias)` was added alongside the existing no-alias version. When `apiConfigAlias` is non-null, non-blank, and not `"default"`, the `setEndPoint` test step's Condition column is set to `#<alias>`:

```
setEndpointStep.setCondition("#" + apiConfigAlias.trim());
```

This leverages the existing Engine mechanism in `Webservice.setEndPoint()` (line 874–890): a Condition starting with `#` causes the Engine to call `driverProperties.setCurrLoadedAPIConfig(alias)` before the request is sent, loading all settings for that config including `useProxy`, `proxyHost`, and `proxyPort`. No Engine changes were required.

When `alias` is `"default"` (or null), the Condition column is left empty and the default config is loaded automatically — the proxy details are still saved to that config.

**`APITester` — new helper: `saveProxyToApiConfig(ProxyConfig, String alias)`**

Persists `useProxy=true`, `proxyHost`, and `proxyPort` into the target API configuration via `DriverProperties`:

1. Resolves alias to `"default"` when blank/null.
2. Calls `driverProps.addAPIName(alias)` + `driverProps.addAPIProperty(alias)` if the config does not already exist (for new aliases).
3. Gets the `Properties` object via `driverProps.getAPIPropertiesFor(alias)`, sets the three proxy keys, then calls `driverProps.save()` to persist to disk.

Access path: `mainFrame.getProject().getProjectSettings().getDriverSettings()` → `DriverProperties`.

New imports added to `APITester`: `com.ing.datalib.settings.DriverProperties`, `java.util.Properties`.

---

### Files changed

| File | Status |
|---|---|
| [Datalib/src/main/java/com/ing/datalib/api/ProxyConfig.java](Datalib/src/main/java/com/ing/datalib/api/ProxyConfig.java) | **New** |
| [Datalib/src/main/java/com/ing/datalib/api/APIRequest.java](Datalib/src/main/java/com/ing/datalib/api/APIRequest.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/request/RequestPanel.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/request/RequestPanel.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/util/APIHttpClient.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/util/APIHttpClient.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/APITester.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/APITester.java) | Modified |

**Breaking changes**: none.

---

## 2. API Workbench — Curl Paste Restricted Header Fix

### What changed

Pasting a `curl` command that included restricted headers such as `Host`, `Connection`, or `Content-Encoding` previously threw:

```
Error: restricted header name: "Host"
```

and prevented the request from being sent at all. All five launcher scripts and the HTTP client class were updated so these headers are accepted. Two client-managed headers (`Content-Length`, `Accept-Encoding`) are silently dropped to avoid corrupted responses.

---

### Root cause

Java's `java.net.http.HttpClient` maintains a hard-coded set of restricted headers that it refuses to let callers set. The restriction exists to keep HTTP/1.1 protocol semantics correct. Curl commands copied from browser DevTools routinely include `Host`, `Connection`, `Accept-Encoding`, and `Content-Length`, making direct replay impossible without this fix.

The JDK provides an official escape hatch — the system property `jdk.httpclient.allowRestrictedHeaders` — but it is read **once**, when the internal HTTP classes first initialise. It therefore must be set before the first `HttpClient` is created anywhere in the JVM, not just before the API tester sends a request.

---

### Launcher scripts (primary mechanism)

`-Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding` was added to every `java`/`javaw` invocation across all five launcher files, covering both the IDE and CLI branches:

| File | Branch |
|---|---|
| [Resources/ingenious](Resources/ingenious) | IDE (no args) + CLI (args) |
| [Resources/ingenious.command](Resources/ingenious.command) | IDE (no args) + CLI (args) |
| [Resources/ingenious.bat](Resources/ingenious.bat) | IDE (no args) + CLI (args) |
| [Resources/Engine/ingenious](Resources/Engine/ingenious) | CLI only |
| [Resources/Engine/ingenious.bat](Resources/Engine/ingenious.bat) | CLI only |

Setting the flag here guarantees it is in effect before any class loads, including the AI-chat and GitHub-auth clients that also create `HttpClient` instances on startup.

---

### `APIHttpClient` — defensive fallback (secondary mechanism)

For contexts where the application is not started through the launcher scripts (IDE/debugger, unit tests, direct `java -jar`), a `static {}` block in `APIHttpClient` calls `relaxRestrictedHeaders()`, which:

1. Reads the current value of `jdk.httpclient.allowRestrictedHeaders` (if already set by the launcher, this is a no-op merge).
2. Adds the same list of header names using a `LinkedHashSet` to deduplicate.
3. Writes the merged value back with `System.setProperty(...)`.

Because the API tester is typically the first component in the JVM to create an `HttpClient` when running outside the launcher, the timing works correctly for the fallback as well.

---

### `APIHttpClient` — fail-soft per-header try/catch

Even with the property set, future JDK versions may narrow or change the restriction list. The `addHeaders()` loop now wraps each `builder.header(name, value)` call in a `try/catch(IllegalArgumentException)`. If a header is still rejected, it is logged at `WARNING` level and skipped rather than throwing and failing the whole request.

---

### Client-managed headers silently dropped

Two headers in the allow-list require special handling beyond just permitting them:

| Header | Reason for dropping |
|---|---|
| `Content-Length` | The `HttpClient` computes this automatically from the body publisher. A manually supplied value can conflict and cause protocol errors. |
| `Accept-Encoding` | Forwarding `gzip` / `deflate` / `br` instructs the server to compress the response, but the JDK client only auto-decompresses when it controls this header itself. Forwarding it produces a compressed (binary/garbled) response body in the Workbench response viewer. |

These two are intercepted in `addHeaders()` before the `builder.header()` call and logged at `FINE` level:

```java
if ("content-length".equalsIgnoreCase(name) || "accept-encoding".equalsIgnoreCase(name)) {
    LOG.log(Level.FINE, "Dropping client-managed request header: " + name);
    continue;
}
```

All other headers in the allow-list (including `Host`, `Connection`) are forwarded as supplied.

---

### Files changed

| File | Status |
|---|---|
| [Resources/ingenious](Resources/ingenious) | Modified |
| [Resources/ingenious.command](Resources/ingenious.command) | Modified |
| [Resources/ingenious.bat](Resources/ingenious.bat) | Modified |
| [Resources/Engine/ingenious](Resources/Engine/ingenious) | Modified |
| [Resources/Engine/ingenious.bat](Resources/Engine/ingenious.bat) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/util/APIHttpClient.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/util/APIHttpClient.java) | Modified |

**Breaking changes**: none.

---

## 3. Web Object Repository — `JSPath` Locator Attribute

### What changed

A new locator attribute, **`JSPath`**, has been added to every Web Object Repository (WebOR) element. It lets users identify an element using a browser DevTools "Copy → Copy JS path" expression (e.g. `document.querySelector("#app > div > button")`). Because this strategy maps to Playwright's `ElementHandle`-style evaluation — which lacks auto-waiting and is brittle compared with `Locator`-based strategies — the attribute is rendered in the Object Properties table with a clearly identifiable **`[Discouraged]`** marker. Its **Exact** column is disabled, consistent with the other selector-based attributes (`xpath`, `css`, `TestId`).

Reference: [Playwright `ElementHandle` (Java)](https://playwright.dev/java/docs/api/class-elementhandle).

---

### Data model (`Datalib`)

**`WebOR.OBJECT_PROPS`**

`JSPath` was appended to the default `OBJECT_PROPS` list, so every new `WebORObject` is created with the attribute and all generic UI/persistence paths pick it up automatically. It is added after `ChainedLocator`:

```java
public static final List<String> OBJECT_PROPS = new ArrayList<>(
    Arrays.asList(
        "Role", "Text", "Label", "Placeholder", "xpath", "css",
        "AltText", "Title", "TestId", "ChainedLocator", "JSPath"
    )
);
```

**`WebORObject.isCellEditable(...)`**

The **Exact** column (column 2) is now non-editable for `JSPath`, alongside the existing `xpath`, `css`, and `TestId` exclusions. The Exact checkbox renders greyed-out/disabled for these selector-based attributes.

**`YamlElementDefinition` (YAML OR format)**

`JSPath` was promoted to a first-class field so it round-trips correctly when the OR is persisted in YAML format (XML already round-trips it as a generic `<Property>`):

- New `private String jsPath` field with `getJsPath()` / `setJsPath(...)`.
- Added to `@JsonPropertyOrder` after `chainedLocator`.
- `fromWebORObject(...)` maps the `jspath` attribute → `jsPath`.
- `toWebORObject(...)` writes `jsPath` back via `setAttributeIfPresent(obj, "JSPath", jsPath, isExact("jspath"))`.
- `hasLocators()` now also considers `jsPath`.

---

### UI (`IDE` — `PropertyAttributeRenderer`)

The Object Properties attribute renderer was extended to flag discouraged attributes:

- A `DISCOURAGED_ATTRIBUTES` set (currently `{"JSPath"}`) and a `DISCOURAGED_MARKER` (`"Discouraged"`) constant were added, with an amber/warning color (`CLR_JSPATH`) and a JavaScript icon (`MaterialDesignL.LANGUAGE_JAVASCRIPT`) registered for `JSPath`.
- When an attribute is discouraged, the cell renders the name followed by a bold red **`[Discouraged]`** badge (HTML label), swaps the icon for a warning/alert icon (`MaterialDesignA.ALERT`), and sets an explanatory tooltip pointing to the Playwright `ElementHandle` docs and recommending `Role` / `Text` / `Label` / `css` / `xpath` instead.
- A small `escapeHtml(...)` helper was added to safely render attribute names inside the HTML label/tooltip.

---

### Element identification (`Engine` — `AutomationObject`)

`JSPath` identification was wired into the locator resolution pipeline:

- A `case "JSPath"` was added to **both** the `Locator` and `FrameLocator` switch statements in `getElements(...)`, calling the new `createJSPathLocator(...)` helpers.
- `extractJSPathSelector(String)` parses a JSPath expression: it extracts the inner selector(s) of one or more `querySelector(...)` / `querySelectorAll(...)` calls (joining multiple calls with a descendant combinator) and falls back to treating the input as a raw CSS selector when no `querySelector` wrapper is present.
- `createJSPathLocator(...)` wraps the extracted selector in a Playwright CSS locator (`page.locator("css=" + selector)` / `framelocator.locator("css=" + selector)`), so the resolved element retains Playwright's auto-waiting behaviour rather than using a raw `ElementHandle`.

Because resolution flows through the existing `getElementsInternal(...)` → `getRuntimeValue(...)` path uniformly for every attribute, the `setObjectProperty` runtime placeholder-substitution mechanism applies to `JSPath` values exactly as it does for `xpath`/`css` — no additional wiring was required.

---

### Files changed

| File | Status |
|---|---|
| [Datalib/src/main/java/com/ing/datalib/or/web/WebOR.java](Datalib/src/main/java/com/ing/datalib/or/web/WebOR.java) | Modified |
| [Datalib/src/main/java/com/ing/datalib/or/web/WebORObject.java](Datalib/src/main/java/com/ing/datalib/or/web/WebORObject.java) | Modified |
| [Datalib/src/main/java/com/ing/datalib/or/yaml/YamlElementDefinition.java](Datalib/src/main/java/com/ing/datalib/or/yaml/YamlElementDefinition.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/utils/table/PropertyAttributeRenderer.java](IDE/src/main/java/com/ing/ide/main/utils/table/PropertyAttributeRenderer.java) | Modified |
| [Engine/src/main/java/com/ing/engine/drivers/AutomationObject.java](Engine/src/main/java/com/ing/engine/drivers/AutomationObject.java) | Modified |

**Breaking changes**: none.

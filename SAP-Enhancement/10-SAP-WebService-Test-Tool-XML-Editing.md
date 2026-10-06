# User Story: [SAP] Assert and Edit XML in SAP's Embedded Document Viewer (Web Service Test Tool)

Source: New request — no Excel item / formal user story yet. Captured here as request #10 to
keep it tracked alongside the other SAP enhancements.

> **Correction from the original ask.** A sample screenshot of the actual element was provided
> after this doc was first drafted. It shows the **"Test Service Provider: Display Response"**
> screen (the Web Service / SOAP test tool reached from `SOAMANAGER`/`WSADMIN`/`SPROXY`'s Test
> function), not the DMEE Format Builder's column tree originally assumed. The "document tree"
> text visible in the screenshot (*"This XML file does not appear to have any style information
> associated with it. The document tree is shown below."*) is the standard message a
> Trident/IE-based browser control shows when rendering raw XML with no XSLT - this is the
> generic default XML viewer, not a SAP-specific tree control at all. The title and filename
> (originally `10-SAP-DMEE-XML-Tree-Editing.md`) have been corrected to match once this became
> clear; the underlying ask (assert/edit XML shown in SAP's "document tree" view) is unchanged.

## User Story

As an INGenious engineer, I want to assert and edit the XML content shown in SAP's embedded Web
Service Test Tool, so that these values can be verified and composed through automation instead
of manual inspection and copy-pasting.

## Acceptance Criteria (ATDD)

### Scenario 1: Capture the Response tab's XML

**Given** the Web Service Test Tool's Response tab is open and showing an XML payload

**When** the content is captured

**Then** it is available as a clean XML string for assertion or storage, with no
browser-viewer rendering artifacts (e.g. collapse/expand glyphs) present

### Scenario 2: Assert captured XML content

**Given** XML content has been captured from the screen

**When** an XPath-based assertion is executed against it

**Then** the actual value at the given path matches the expected value

### Scenario 3: Edit the Request tab's XML

**Given** the Web Service Test Tool's Request tab is open

**When** an edit action is executed with a full XML document as input

**Then** the field's content is replaced with the supplied XML, and malformed XML is rejected
before anything is written

### Scenario 4: Fallback capture when direct DOM access is unavailable

**Given** the embedded viewer's content cannot be read through SAP GUI Scripting's ordinary
element properties

**When** a select-all + copy based capture is executed instead

**Then** the on-screen text is captured and cleaned of the viewer's own collapse-glyph and
spacing artifacts before being stored

### Scenario 5: Documentation is updated

**Given** this capability is implemented

**When** documentation is updated

**Then** setup instructions and usage examples are available in ingenious-doc

## Related SAP Request Items

None — this request has no corresponding row in `SAP Ingenious Issues 2026.xlsx`. Raised
directly during this session.

## Context

The actual target screen (per the provided screenshot) is SAP's **Web Service Test Tool** -
"Test Service Provider: Display Response" - reached from `SOAMANAGER`/`WSADMIN`/`SPROXY`'s Test
function. Its Response tab renders the raw SOAP/XML payload using a browser control's default,
unstyled XML view (the "document tree is shown below" message is literally the browser's own
fallback renderer, not a SAP-authored control). This is very likely a `GuiHTMLViewer` - SAP GUI
Scripting's embedded-browser object type - hosting an IE/Trident `HTMLDocument`, which is a
different automation surface entirely from the plain parent/child `GuiTree` already scripted
elsewhere in this codebase (tree navigation by node key/text, see
`SAP-Enhancement/05-SAP-Dynamic-Object-Selection-Tables-Tree.md`) and from DMEE's Format
Builder column tree originally assumed. No code in this repo references `GuiHTMLViewer` today.

The same test tool's **Request tab** is the editable counterpart raised for Scenario 3 -
unlike Response, it must accept a user-typed or pasted SOAP/XML body before the test call is
sent, so it is far more likely implemented as a plain multi-line `GuiTextEdit` (standard SAP
GUI Scripting text-entry control) rather than a `GuiHTMLViewer` - embedded browsers are a
presentation surface, not an input one, so SAP would not normally use one for free-form typed
input. This distinction matters: reading (Response tab) and writing (Request tab) are most
likely two different control types requiring two different automation approaches, even though
both hold "XML" conceptually.

## Goal

Provide the ability to capture a DMEE document tree's structure as XML, assert against it via
XPath, and edit individual node values/attributes.

## Value

- Enables automated verification of DMEE format configurations instead of manual inspection.
- Extends INGenious's existing XML/XPath assertion capability (already used for Webservice/API
  responses) to SAP GUI content.
- Supports configuration-audit and regression scenarios for SAP interface mappings.

## Scope

Affected Components:

- SAP Automation Framework
- SAP UI Validation Actions
- Validation Framework
- Workflow Actions
- Reporting Engine

## Current State (gap analysis)

**The "document tree" is a browser-rendered XML view, not a SAP tree control at all.** The
screenshot's banner text is IE/Trident's standard no-stylesheet XML fallback message. This
means none of the existing `GuiTree`-based actions (`sapExpandTreeNode`, `sapSelectTreeNode`,
`sapNavigateTreePath`, `sapSelectTreeNodeByText(Contains)`, etc.) apply here at all — they
script a completely different control type. The relevant SAP GUI Scripting object is almost
certainly `GuiHTMLViewer` (the embedded-browser element type), not `GuiTree`/`GuiShell`.

**No code in this repo touches `GuiHTMLViewer` today**, so there is no existing partial
coverage to build on, unlike the table/grid/tree gaps analyzed elsewhere in this folder — this
is a new element type for the codebase, not an extension of one already scripted.

**`GuiHTMLViewer` is a different, narrower JACOB surface than the rest of SAP GUI Scripting.**
Every other `SAPElement` interaction in `SAPActions.java` calls `Dispatch.get`/`Dispatch.call`
directly on a `GuiVComponent`-family object (`Text`, `Selected`, `RowCount`, ...).
`GuiHTMLViewer` instead exposes a `GetHTMLDocument()` method that — when it works — returns a
COM `HTMLDocument` object one level removed from the SAP GUI Scripting tree, through which the
page's DOM is reachable (e.g. an IE/Trident `HTMLDocument`'s root XML node exposes an `.xml`
property that serializes the whole document back to a string). Reading from it is a plausible,
previously-undocumented JACOB call chain (`Dispatch.call(htmlViewer, "GetHTMLDocument")` then
further `Dispatch.get`/`.call` on the returned `HTMLDocument`/`IXMLDOMNode`), but it is **not
proven in this codebase** and is known in general SAP GUI Scripting practice to be
version/browser-engine dependent (IE/Trident vs. the newer Edge/WebView2-based `GuiHTMLViewer`
in recent SAP GUI releases expose this differently, and some environments disable the method
entirely for security reasons).

**A full XPath-based XML assertion engine already exists and is proven — just never wired to
SAP.** `StructuredData.java` (`Engine/src/main/java/com/ing/engine/commands/structuredData/StructuredData.java`)
already implements `assertXmlPathResultContains` and related actions using
`javax.xml.xpath`/`javax.xml.parsers` against JSON/XML Webservice responses. Once the raw XML
string is obtained from the screen by whatever means, **this existing engine is directly
reusable with zero new XML-parsing code** — the entire gap is in getting the string out of the
`GuiHTMLViewer`, not in asserting against it afterward.

**The Response tab itself is read-only** — there is no visible edit affordance for the XML
shown there. Scenario 3 instead targets the Request tab's editable XML entry field, which is a
separate control from the one analyzed for Scenario 1/2 above.

**Confirmed: a plain select-all + copy off the Response tree view does not yield clean XML as-is.**
A real captured sample (SAP `PaymentTransactionOrderFSCreateRequest` payload) copied from this
kind of viewer carries a literal `-` (or `+`) character in front of every container-element
line — the collapse/expand glyph IE/Trident renders in the margin of its default no-stylesheet
XML view, picked up by the copy because it's part of the same line's text, not a separate
margin element. The underlying content is otherwise well-formed once those glyphs (and the
viewer's blank spacing lines) are stripped. This confirms proposed action #3's fallback is
viable, but only once that cleanup step runs on the captured text — a plain clipboard dump
alone is not safe to feed straight into an XML parser or write back into a request field.

**Writing a whole field's content already has a direct precedent: `sapFill`.**
`sapFill` (`Dispatch.put(SAPElement, "Text", Data)`) already replaces an entire element's
`Text` property in one call and is control-agnostic — it works on any `GuiVComponent` exposing
a writable `Text`, which a multi-line `GuiTextEdit` does. If the Request tab is indeed a plain
`GuiTextEdit`, **`sapFill` may already satisfy Scenario 3 with zero new code**, the same
conclusion reached for DMEE-style node editing elsewhere in this doc. The one thing `sapFill`
does *not* do is validate that `Data` is well-formed XML before writing it — it will happily
write malformed text into the field and let the failure surface later (SAP GUI error, or a
confusing downstream test-call failure) instead of failing fast at the edit step itself.

## Proposed Actions

Status below reflects what could be implemented **without** a SAP GUI Scripting Recorder
session (per the recording-blocked note above) versus what remains blocked on it.

1. **`sapStoreHTMLViewerXmlInVariable`** — ⛔ **blocked, needs recording.** Calls
   `GetHTMLDocument()` on the `GuiHTMLViewer` element, then reads the serialized XML off the
   returned COM document object (exact property/method TBD by recorder - e.g. `.xml` on the
   root node for a Trident `HTMLDocument`). Not implemented — depends on confirming
   `GetHTMLDocument()` actually works against the real control.
2. **Reuse the existing `StructuredData` XPath engine for assertions** — ⛔ **deferred, not
   implemented yet.** Intentionally held back rather than guessed at: its design (what it reads
   from - a variable already populated by #1/#3, or the element directly) depends on how value
   capture is ultimately wired, which is itself blocked on #1. Implementing it now would risk
   committing to an API shape that conflicts with the real capture mechanism once confirmed.
3. **Fallback capture via select-all + copy** — ✅ **implemented** as
   `sapCaptureElementTextViaClipboard` (`SAPActions.java`): focuses the element, sends
   Ctrl+A/Ctrl+C via `Robot`, reads the OS clipboard, and runs the result through a new
   `stripXmlTreeViewArtifacts()` helper that removes the confirmed `-`/`+` collapse-glyph
   prefixes and blank spacing lines before storing into a variable. Control-agnostic — doesn't
   depend on confirming `GuiHTMLViewer` vs. any other control type, so needed no recording.
4. **`sapSetElementXMLEditor`** (renamed from the original `sapSetElementXmlText`) — ✅
   **implemented** (`SAPActions.java`): `Object` + `Data`, `Data` typed as `ArgType.XML_BODY` so
   the IDE renders it with the existing raw-body/XML editor treatment already used for
   Webservice JSON/XML bodies. Validates `Data` is well-formed XML (via a hardened
   `DocumentBuilderFactory` - DOCTYPE and external entities disabled to avoid XXE) before
   writing it to the element's `Text` property, failing fast with a parse error instead of
   silently writing malformed XML. Safe to implement without recording because it writes via
   the same `Text`-property mechanism `sapFill` already uses successfully against every other
   scripted control in this codebase.

## Open Questions

- **Does `GuiHTMLViewer.GetHTMLDocument()` actually work against this SAP GUI version/build?**
  Not resolved — this is the single blocking unknown. Needs a SAP GUI Scripting Recorder session
  against the real "Test Service Provider: Display Response" screen (or equivalent) to confirm
  the method exists, returns a usable COM object, and isn't blocked by security policy.
- **What COM interface does the returned document expose** — classic Trident `IHTMLDocument2`
  (`.xml` on a selected/root XML node) or something else if the SAP GUI build uses a
  WebView2/Edge-based viewer instead? Determines the exact property/method chain for proposed
  action #1.
- **Is there an editable counterpart screen this request also needs to cover?** Assumed to be
  the same test tool's Request tab, not yet screenshotted/confirmed.
- **Is the Request tab a plain `GuiTextEdit`/`GuiTextField`, or also a `GuiHTMLViewer`-style
  control?** Determines whether proposed action #4 is genuinely new work (`GetHTMLDocument()`
  write path) or mostly validation layered on the existing `sapFill` write path. Not confirmed
  — needs a recorder session against the actual Request tab.
- **Is there a practical size limit** on how much XML can be written via `Text`/`sapFill` for
  very large SOAP payloads (multi-line control scrolling/performance), or does it need to be
  chunked/typed incrementally instead of set in one call? Not confirmed.


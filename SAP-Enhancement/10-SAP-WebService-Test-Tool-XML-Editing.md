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

> **Toggle modes - final confirmed state, after several back-and-forth corrections.** The
> Request tab has two toolbar buttons: **"XML Editor" (Ctrl+F2)** and **"HTML Control"
> (Ctrl+F3)**. Confirmed directly, across multiple screenshots:
> - **"XML Editor"** = the **editable** mode ("Change Request") - a plain multi-line text box
>   with ordinary line numbers (`Ln`/`Col` status bar), no tree, no collapse/fold markers.
> - **"HTML Control"** = the **read-only** mode ("Display Request"/"Display Response") - the
>   collapsible `-`/`+` tree view analyzed earlier in this doc (the one carrying the
>   confirmed collapse-glyph copy artifact). This restores this doc's very first hypothesis
>   that the tree/document view is the `GuiHTMLViewer`-style control - it's just reached via
>   "HTML Control" specifically, not whichever button happened to be highlighted at the time.
>
> Net effect: **edit via "XML Editor"** (plain text box, good match for
> `sapSetElementXMLEditor`'s existing `Text`-property write), **capture/read via "HTML
> Control"** (tree view, needs `stripXmlTreeViewArtifacts()`-style cleanup, and remains the
> target for the still-blocked `GetHTMLDocument()`-based read). This superseded two earlier,
> contradictory intermediate guesses in this doc (that "HTML Control" was editable; that "XML
> Editor" had code-folding) - both retracted.

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

The actual target screen (per the provided screenshots) is SAP's **Web Service Test Tool** -
"Test Service Provider: Display Response"/"Display Request" - reached from
`SOAMANAGER`/`WSADMIN`/`SPROXY`'s Test function. Both its Request and Response tabs can show
either of two toggled views, confirmed via toolbar buttons:

| Mode | Toolbar button | Shortcut | What it shows | Editable? |
|---|---|---|---|---|
| Collapsible tree view | "HTML Control" | Ctrl+F3 | The `-`/`+` collapsible document-tree rendering (the "document tree is shown below" no-stylesheet XML view) | **No - read-only**, used in "Display Request"/"Display Response" |
| Plain line-numbered text editor | "XML Editor" | Ctrl+F2 | A simple multi-line text box showing the raw XML with line numbers (`Ln`/`Col` status bar) - no collapse/fold markers | **Yes - this is the editable mode**, used in "Change Request" |

This confirms this doc's original hypothesis about the tree/document view: it's reached via
**"HTML Control"** specifically, and is very likely a `GuiHTMLViewer` - SAP GUI Scripting's
embedded-browser object type - hosting an IE/Trident `HTMLDocument`, a different automation
surface entirely from the plain parent/child `GuiTree` already scripted elsewhere in this
codebase (tree navigation by node key/text, see
`SAP-Enhancement/05-SAP-Dynamic-Object-Selection-Tables-Tree.md`). No code in this repo
references `GuiHTMLViewer` today.

**"XML Editor"**, confirmed by direct screenshot, is a plain multi-line text box with ordinary
line numbers - no tree, no fold/collapse markers, nothing more exotic. This is the closest
match yet to a standard `GuiTextEdit`, so it's very likely to expose a single whole-buffer
`Text` property - meaning `sapSetElementXMLEditor`'s existing flat `Text`-property write
(`Dispatch.put(SAPElement, "Text", Data)`) is expected to work directly against it.

Practically: **capture/read via "HTML Control"** (tree view - needs the confirmed
collapse-glyph cleanup, `stripXmlTreeViewArtifacts()`), **edit via "XML Editor"** (plain text
box, the confirmed target for Scenario 3, via the already-implemented `sapSetElementXMLEditor`).
This reading/writing split through two different controls, reached by clicking the
corresponding toolbar button first, replaces this doc's earlier assumption that Request and
Response were simply two different fixed controls - both tabs can toggle between the same two
modes.

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

**The Response tab is read-only regardless of mode** — there is no edit affordance there at
all. Scenario 3 targets the Request tab specifically, and within it, the "XML Editor" mode
specifically (see Context) — a separate control from the "HTML Control" tree view analyzed for
Scenario 1/2 above.

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
`sapFill` (`Dispatch.put(SAPElement, "Text", Data)`) already replaces an entire element's `Text`
property in one call and is control-agnostic - it works on any `GuiVComponent` exposing a
writable `Text`. The editable mode, **"XML Editor"**, is confirmed by screenshot to be a plain
multi-line text box with ordinary line numbers (no tree, no fold/collapse markers) - the
closest match yet to a standard `GuiTextEdit`-style control, so `sapSetElementXMLEditor`'s
existing whole-field `Text`-property write is expected to apply directly, the same conclusion
reached for DMEE-style editing elsewhere in this doc. Still not proven against a real session
(see Open Questions), but no longer expected to need per-node or structural interaction.

**No new action needed to switch modes - the existing generic click actions already cover it.**
Once the "XML Editor"/"HTML Control" toggle buttons are captured as ordinary toolbar buttons in
the SAP Object Repository (they're regular `GuiButton`-family toolbar icons, not XML-specific),
the already-existing `sapClick`/`sapPressButton` actions (`SAPActions.java`) can switch modes
before running any read/write step - no new SAP action code required for this part. The
recommended sequence becomes: click **"HTML Control"** → run a capture action (read, off the
tree view) against that now-active read-only control; separately, click **"XML Editor"** → run
`sapSetElementXMLEditor` (write) against that now-active editable text box.

**The fallback-capture gap targets the correct mode.** `sapCaptureElementTextViaClipboard`'s
`stripXmlTreeViewArtifacts()` cleanup exists for copying out of the **"HTML Control"** tree
view, where the `-`/`+` glyphs are confirmed present - that remains unchanged by the mode-naming
correction. What's corrected is simply which button reaches which behavior: capture/read via
"HTML Control" (tree view, needs the glyph cleanup), edit via "XML Editor" (plain text box, no
cleanup needed since it was never written to from captured/stripped text).

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
  Partially de-risked, still not proven: "HTML Control" mode being explicitly SAP-labeled (and
  confirmed read-only, consistent with a passive rendering control) makes it a much stronger
  candidate for actually being a scriptable `GuiHTMLViewer` than this doc's original guess - but
  the method call itself is still unverified against a real session. Needs a SAP GUI Scripting
  Recorder session, specifically **after** switching to "HTML Control" mode (used for capture,
  not editing).
- **What COM interface does the returned document expose** — classic Trident `IHTMLDocument2`
  (`.xml` on a selected/root XML node) or something else if the SAP GUI build uses a
  WebView2/Edge-based viewer instead? Determines the exact property/method chain for proposed
  action #1.
- **Is there an editable counterpart screen this request also needs to cover?** — ✅ resolved:
  confirmed to be the same test tool's Request tab, specifically its **"XML Editor"** mode (a
  plain line-numbered text box) - corrected from this doc's earlier guesses of "HTML Control",
  then "a tree control", then "a folding text editor".
- **Does `sapSetElementXMLEditor`'s whole-field `Text`-property write actually work against the
  "XML Editor" text box?** — the central open question for Scenario 3, now the **least**
  uncertain it's been across this doc's several corrections: a plain multi-line text box with
  simple line numbers is the closest match yet to a standard `GuiTextEdit`, so it's expected to
  expose a single whole-buffer `Text` property compatible with the existing write approach.
  Still not proven against a real session - needs a recorder session against the real "XML
  Editor" mode to confirm the property name/behavior.
- **Which toolbar button is "HTML Control" vs "XML Editor" in the Object Repository, and does
  clicking it reliably toggle the same underlying element, or does the element itself change
  identity between modes** (i.e. does the OR need two separate captured objects, one per mode,
  rather than one element whose view just changes)? Not confirmed - affects whether the
  "click to switch, then act" workflow described above needs one OR object or two.
- **Is there a practical size limit** on how much XML can be written via `Text`/`sapFill` for
  very large SOAP payloads (multi-line control scrolling/performance), or does it need to be
  chunked/typed incrementally instead of set in one call? Not confirmed.


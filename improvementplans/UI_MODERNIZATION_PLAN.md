# UI Modernization Recommendations

**Date:** February 14, 2026  
**Current State:** Java Swing + JavaFX hybrid, Nimbus Look & Feel with heavy customization  
**IDE module:** 212 source files  

---

## Current Architecture

- **Primary framework:** Java Swing (JFrame, JPanel, JTree, JTable, JTabbedPane)
- **Secondary framework:** JavaFX (embedded via `javafx-swing` bridge for web views, media)
- **Look & Feel:** Nimbus with ~100 tweaked UIManager keys in `Main.java`
- **Custom font:** ING Me (loaded from TTF at runtime)
- **Brand colors:** ING Orange `#FF6200`, warm neutrals, purple text `#583A74`
- **Custom painters:** `FillPainter1` inner class for flat rendering on Nimbus
- **Dark theme:** Partially implemented (`tweakNimbusDarkUI()` exists but unused)

---

## Modernization Options

### Option 1 — FlatLaf Drop-In Replacement (1-2 days) ✅ IMPLEMENTED

**Swap Nimbus for [FlatLaf](https://www.formdev.com/flatlaf/)** — a modern, flat Look & Feel for Swing.

- Add one dependency: `com.formdev:flatlaf:3.5.4`
- Replace `setUpUI("Nimbus")` → `FlatLightLaf.setup()`
- Instantly modernizes all Swing components — rounded corners, better spacing, crisp fonts, HiDPI support
- Supports **light/dark/IntelliJ/Darcula** themes out of the box
- Minimal code changes — most custom Nimbus painters can be removed
- Custom app-specific color keys preserved for component code that references them
- Used by JetBrains, DBeaver, and other major Java desktop apps

**Changes made:**
1. Added `com.formdev:flatlaf:3.5.4` dependency to `IDE/pom.xml`
2. Replaced Nimbus L&F initialization with `FlatLightLaf.setup()` in `Main.java`
3. Replaced `tweakNimbusUI()` with `applyCustomColors()` — preserves app-specific color keys (`tableColor`, `subToolBar`, `toolBar`, `execPanel`, etc.) and custom ING Me font registration
4. Replaced `tweakNimbusDarkUI()` with `applyDarkCustomColors()` — ready for dark theme toggle
5. Removed `FillPainter1` inner class and all Nimbus-specific painter keys (not needed with FlatLaf)
6. Added FlatLaf-native customizations: accent color, selection colors, tab styling

### Option 2 — FlatLaf + Custom Theme (3-5 days, future)

- Everything from Option 1, plus:
- Create a custom `.properties` theme file matching ING brand colors
- Add a **theme toggle** (light/dark) in the toolbar or settings
- Replace hardcoded icon PNGs with **SVG icons** (FlatLaf supports SVG natively)
- Apply consistent spacing and padding via FlatLaf's `UIDefaultsLoader`

### Option 3 — Gradual JavaFX Migration (weeks-months, future)

- Replace Swing panels one-at-a-time with JavaFX scenes
- Start with high-visibility panels: test execution dashboard, settings dialogs, report viewer
- Use CSS for styling — much easier to theme than Swing
- Long-term path to a fully modern UI, but massive effort for 212 files

### Option 4 — Web-Based UI (months, high risk, future)

- Replace the desktop app with an Electron/Tauri + React/Angular frontend
- Engine and Datalib modules stay as-is (backend)
- Communicate via REST API or IPC
- Maximum modern UX, but essentially a rewrite of the IDE module

---

## Recommendation Path

```
Option 1 (now)      ████ FlatLaf drop-in                    ✅ Done
Option 2 (next)     ████████ Custom theme + dark mode       Future sprint
Option 3 (later)    ████████████████ JavaFX migration        Long-term
```

**Option 1 gives 80% of the visual improvement for 5% of the effort.**

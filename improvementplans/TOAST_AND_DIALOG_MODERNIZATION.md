# Toast Messages and Confirmation Dialogs Modernization

## Overview

All toast messages and confirmation dialogs in INGenious have been modernized with a sleek, contemporary design featuring rounded corners, shadows, modern colors, and intuitive icons.

---

## 🎨 Key Improvements

### Toast Notifications

#### Visual Enhancements
- **Rounded Corners**: Smooth 12px radius for modern appearance
- **Shadow Effect**: Subtle multi-layer shadow for depth perception
- **Icons**: Contextual emoji icons for each notification type:
  - ✓ Success (green)
  - ⓘ Info (blue)
  - ⚠ Warning (orange)
  - ✕ Error (red)
- **Glass Effect**: Semi-transparent background (240 alpha) for modern aesthetics
- **Better Typography**: Segoe UI font family with improved sizing

#### Color Scheme
```java
SUCCESS:  #4CAF50 (Green)
INFO:     #2196F3 (Blue)
WARNING:  #FF9800 (Orange)
ERROR:    #F44336 (Red)
```

#### Automatic Type Detection
The notification system now intelligently detects message types based on keywords:

```java
// Success keywords: "saved", "created", "success", "added", "done", "copied", "renamed", "complete", "loaded"
Notification.show("Project saved successfully"); // Shows green success toast

// Error keywords: "error", "failed", "couldn't", "could not", "unable", "invalid"
Notification.show("Failed to load file"); // Shows red error toast

// Warning keywords: "warning", "already", "overwrite", "conflict"
Notification.show("File already exists"); // Shows orange warning toast

// Everything else defaults to info (blue)
Notification.show("Loading project..."); // Shows blue info toast
```

### Confirmation Dialogs

#### Visual Enhancements
- **Modern Card Design**: Floating dialog with rounded corners and shadow
- **Draggable**: Click and drag anywhere on the dialog to move it
- **Styled Buttons**: Rounded, hover-responsive buttons with smooth transitions
- **Color-Coded**: Dialog color matches the message type (warning, error, info)
- **Icons**: Large emoji icons for visual context
- **ESC to Close**: Press ESC key to cancel

#### Features
- Undecorated window with custom title bar
- Smooth animations and hover effects
- Modern color palette
- Responsive button states (normal, hover, pressed)

---

## 📚 API Reference

### Toast Notifications

#### IDE Module (`com.ing.ide.util.Notification`)

```java
// Automatic type detection
Notification.show("Your message");
Notification.show(parentComponent, "Your message");

// Explicit type specification
Notification.showSuccess("Operation completed!");
Notification.showSuccess(parentComponent, "Operation completed!");

Notification.showInfo("Processing data...");
Notification.showInfo(parentComponent, "Processing data...");

Notification.showWarning("File already exists");
Notification.showWarning(parentComponent, "File already exists");

Notification.showError("Failed to save file");
Notification.showError(parentComponent, "Failed to save file");
```

#### StoryWriter Module (`com.ing.storywriter.util.Notification`)

```java
// Same API as IDE module
Notification.show("Your message");
Notification.showSuccess(parent, "Story saved!");
Notification.showInfo(parent, "Loading stories...");
Notification.showWarning(parent, "Scenario already exists");
Notification.showError(parent, "Invalid story format");
```

### Confirmation Dialogs

#### IDE Module (`com.ing.ide.util.Notification` and `com.ing.ide.main.utils.StyledConfirmDialog`)

```java
// Delete confirmation
boolean shouldDelete = Notification.showDeleteConfirmation();
boolean shouldDelete = Notification.showDeleteConfirmation("Delete this item?");

// General confirmation
boolean confirmed = Notification.showConfirm(parent, "Proceed with operation?", "Confirm");

// Warning confirmation
boolean confirmed = Notification.showWarningConfirm(parent, "This will overwrite existing data", "Warning");

// Direct use of StyledConfirmDialog
int result = StyledConfirmDialog.showYesNo(parent, "Continue?", "Confirm", StyledConfirmDialog.CONFIRM);
if (result == StyledConfirmDialog.YES_OPTION) {
    // User clicked Yes
}

// With three options (Yes/No/Cancel)
int result = StyledConfirmDialog.showConfirm(parent, "Save changes?", "Save", StyledConfirmDialog.CONFIRM);
```

#### StoryWriter Module (`com.ing.storywriter.util.Notification` and `com.ing.storywriter.util.StyledConfirmDialog`)

```java
// Same API as IDE module
boolean shouldDelete = Notification.showDeleteConfirmation("Delete scenario?");
boolean confirmed = Notification.showConfirm(parent, "Export stories?", "Export");
```

### Drop-in Replacement for JOptionPane

For easier migration, use `StyledDialogs` class which provides JOptionPane-compatible methods:

#### IDE Module (`com.ing.ide.main.utils.StyledDialogs`)

```java
// Replace JOptionPane.showConfirmDialog with StyledDialogs.showConfirmDialog
int result = StyledDialogs.showConfirmDialog(parent, "Delete this file?", "Delete", 
    StyledDialogs.YES_NO_OPTION, StyledDialogs.WARNING_MESSAGE);

// Replace JOptionPane.showMessageDialog with StyledDialogs.showMessageDialog
StyledDialogs.showMessageDialog(parent, "Operation successful");

// Input dialogs still use JOptionPane (requires text input)
String name = StyledDialogs.showInputDialog(parent, "Enter name:");
```

#### StoryWriter Module (`com.ing.storywriter.util.StyledDialogs`)

```java
// Same API as IDE module
int result = StyledDialogs.showConfirmDialog(parent, "Save changes?", "Save",
    StyledDialogs.YES_NO_OPTION, StyledDialogs.QUESTION_MESSAGE);
```

---

## 🔄 Migration Guide

### Migrating from JOptionPane

#### Before (Old Style)
```java
int result = JOptionPane.showConfirmDialog(parent, 
    "Are you sure?", 
    "Confirm", 
    JOptionPane.YES_NO_OPTION, 
    JOptionPane.WARNING_MESSAGE);

if (result == JOptionPane.YES_OPTION) {
    // Do something
}

JOptionPane.showMessageDialog(parent, "Success!");
```

#### After (New Styled)

**Option 1: Use StyledDialogs (least changes)**
```java
import com.ing.ide.main.utils.StyledDialogs; // or com.ing.storywriter.util.StyledDialogs

int result = StyledDialogs.showConfirmDialog(parent,
    "Are you sure?",
    "Confirm",
    StyledDialogs.YES_NO_OPTION,
    StyledDialogs.WARNING_MESSAGE);

if (result == StyledDialogs.YES_OPTION) {
    // Do something
}

StyledDialogs.showMessageDialog(parent, "Success!");
```

**Option 2: Use Notification methods (recommended)**
```java
import com.ing.ide.util.Notification; // or com.ing.storywriter.util.Notification

boolean confirmed = Notification.showWarningConfirm(parent, "Are you sure?", "Confirm");
if (confirmed) {
    // Do something
}

Notification.showSuccess(parent, "Success!");
```

---

## 🎯 File Changes Summary

### New Files Created

#### IDE Module
- `IDE/src/main/java/com/ing/ide/main/utils/StyledConfirmDialog.java` - Modern confirmation dialog
- `IDE/src/main/java/com/ing/ide/main/utils/StyledDialogs.java` - JOptionPane drop-in replacement

#### StoryWriter Module
- `StoryWriter/src/main/java/com/ing/storywriter/util/StyledConfirmDialog.java` - Modern confirmation dialog
- `StoryWriter/src/main/java/com/ing/storywriter/util/StyledDialogs.java` - JOptionPane drop-in replacement

### Modified Files

#### IDE Module
- `IDE/src/main/java/com/ing/ide/main/utils/toasterNotification/ToasterDialog.java` - Enhanced with rounded corners, shadows, icons
- `IDE/src/main/java/com/ing/ide/main/utils/toasterNotification/Toaster.java` - Added warning and error toast methods
- `IDE/src/main/java/com/ing/ide/util/Notification.java` - Added styled dialog support, warning/error methods
- `IDE/src/main/java/com/ing/ide/util/Utility.java` - Updated to use styled confirm dialog

#### StoryWriter Module
- `StoryWriter/src/main/java/com/ing/storywriter/util/toaster/ToasterDialog.java` - Enhanced with rounded corners, shadows, icons
- `StoryWriter/src/main/java/com/ing/storywriter/util/toaster/Toaster.java` - Added warning and error toast methods
- `StoryWriter/src/main/java/com/ing/storywriter/util/Notification.java` - Added styled dialog support, warning/error methods

---

## 💡 Usage Examples

### Example 1: Save Confirmation
```java
// Old
int result = JOptionPane.showConfirmDialog(this, "Save changes?", "Save", 
    JOptionPane.YES_NO_OPTION);
if (result == JOptionPane.YES_OPTION) {
    saveFile();
    JOptionPane.showMessageDialog(this, "File saved successfully");
}

// New
boolean shouldSave = Notification.showConfirm(this, "Save changes?", "Save");
if (shouldSave) {
    saveFile();
    Notification.showSuccess(this, "File saved successfully");
}
```

### Example 2: Delete Warning
```java
// Old
int result = JOptionPane.showConfirmDialog(null, 
    "Are you sure want to delete?", 
    "Delete", 
    JOptionPane.YES_NO_OPTION, 
    JOptionPane.WARNING_MESSAGE);
if (result == JOptionPane.YES_OPTION) {
    deleteItem();
}

// New
if (Notification.showDeleteConfirmation("Are you sure want to delete?")) {
    deleteItem();
    Notification.showSuccess("Item deleted successfully");
}
```

### Example 3: Error Handling
```java
// Old
try {
    loadFile();
} catch (Exception e) {
    JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), 
        "Error", JOptionPane.ERROR_MESSAGE);
}

// New
try {
    loadFile();
    Notification.showSuccess("File loaded successfully");
} catch (Exception e) {
    Notification.showError("Error: " + e.getMessage());
}
```

### Example 4: Multi-Type Notifications
```java
// Info
Notification.showInfo("Loading projects...");

// Success
Notification.showSuccess("Project created successfully");

// Warning
Notification.showWarning("Project name already exists");

// Error
Notification.showError("Failed to create project");

// Auto-detect (recommended)
Notification.show("Operation completed"); // Auto-detects as success
Notification.show("Invalid input"); // Auto-detects as error
```

---

## 🎨 Customization

### Toast Notification Settings

You can customize toast behavior using the Toaster class:

```java
Toaster toaster = new Toaster();

// Adjust display time (milliseconds)
toaster.setDisplayTime(5000); // 5 seconds

// Adjust animation speed
toaster.setStep(25); // Faster animation
toaster.setStepTime(15); // Smoother animation

// Adjust size
toaster.setToasterWidth(350);
toaster.setToasterHeight(100);
```

---

## 🚀 Benefits

1. **Modern UI/UX**: Contemporary design that matches modern application standards
2. **Better User Feedback**: Clear visual distinction between message types
3. **Improved Readability**: Icons and colors make it easier to understand message severity
4. **Consistent Experience**: Uniform styling across all dialogs and notifications
5. **Enhanced Usability**: Draggable dialogs, keyboard shortcuts, smooth animations
6. **Maintainability**: Centralized styling makes future updates easier
7. **Backward Compatible**: Drop-in replacement utilities maintain existing API

---

## 📝 Notes

- Toast notifications automatically disappear after 3 seconds (configurable)
- Confirmation dialogs support ESC key to cancel
- All dialogs are modal and block interaction until dismissed
- Colors are designed to be accessible and work well in both light and dark themes
- The system intelligently auto-detects message types but explicit methods are available for precise control

---

## 🔮 Future Enhancements

Potential improvements for future versions:
- Dark mode support with theme detection
- Animation customization options
- Toast notification queue management
- Sound effects for different notification types
- Custom icon support
- Notification history/log
- Toast positioning options (top, bottom, left, right)
- Multi-line message formatting with better layout

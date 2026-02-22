# Toast & Dialog Quick Reference

## 🚀 Quick Start

### Toast Notifications

```java
// Auto-detect type (recommended)
Notification.show("Your message here");

// Explicit types
Notification.showSuccess(this, "Operation successful");
Notification.showInfo(this, "Processing data...");
Notification.showWarning(this, "File already exists");
Notification.showError(this, "Failed to save");
```

### Confirmation Dialogs

```java
// Delete confirmation
if (Notification.showDeleteConfirmation("Delete this item?")) {
    // User clicked Yes
}

// General confirmation
if (Notification.showConfirm(this, "Proceed?", "Confirm")) {
    // User clicked Yes
}

// Warning confirmation
if (Notification.showWarningConfirm(this, "Overwrite?", "Warning")) {
    // User clicked Yes
}
```

---

## 📋 API Cheat Sheet

### IDE Module
```java
import com.ing.ide.util.Notification;
import com.ing.ide.main.utils.StyledConfirmDialog;
import com.ing.ide.main.utils.StyledDialogs;
```

### StoryWriter Module
```java
import com.ing.storywriter.util.Notification;
import com.ing.storywriter.util.StyledConfirmDialog;
import com.ing.storywriter.util.StyledDialogs;
```

---

## 🎨 Toast Types & Colors

| Method | Color | Icon | Use Case |
|--------|-------|------|----------|
| `showSuccess()` | 🟢 Green | ✓ | Completed actions |
| `showInfo()` | 🔵 Blue | ⓘ | General information |
| `showWarning()` | 🟠 Orange | ⚠ | Caution messages |
| `showError()` | 🔴 Red | ✕ | Errors & failures |

---

## 🔤 Auto-Detection Keywords

**Success**: saved, created, success, added, done, copied, renamed, complete, loaded

**Error**: error, failed, couldn't, could not, unable, invalid

**Warning**: warning, already, overwrite, conflict

---

## 💡 Common Patterns

### Save Operation
```java
try {
    saveFile();
    Notification.showSuccess(this, "File saved successfully");
} catch (Exception e) {
    Notification.showError(this, "Failed to save: " + e.getMessage());
}
```

### Delete with Confirmation
```java
if (Notification.showDeleteConfirmation("Delete project?")) {
    deleteProject();
    Notification.showSuccess("Project deleted");
}
```

### Input Validation
```java
if (input.isEmpty()) {
    Notification.showWarning(this, "Please enter a name");
    return;
}
```

---

## 🔄 JOptionPane Migration

### Message Dialog
```java
// OLD
JOptionPane.showMessageDialog(this, "Success!");

// NEW
Notification.showSuccess(this, "Success!");
```

### Confirm Dialog
```java
// OLD
int result = JOptionPane.showConfirmDialog(this, 
    "Delete?", "Confirm", 
    JOptionPane.YES_NO_OPTION);
if (result == JOptionPane.YES_OPTION) { ... }

// NEW
if (Notification.showDeleteConfirmation("Delete?")) { ... }
```

### Drop-in Replacement
```java
// Use StyledDialogs
int result = StyledDialogs.showConfirmDialog(this,
    "Continue?", "Confirm",
    StyledDialogs.YES_NO_OPTION,
    StyledDialogs.QUESTION_MESSAGE);
```

---

## ⚙️ Customization

### Toast Display Time
```java
Toaster toaster = new Toaster();
toaster.setDisplayTime(5000); // 5 seconds
```

### Toast Size
```java
toaster.setToasterWidth(350);
toaster.setToasterHeight(100);
```

---

## 🎯 Return Values

### Confirmation Dialogs
```java
StyledConfirmDialog.YES_OPTION    // 0
StyledConfirmDialog.NO_OPTION     // 1
StyledConfirmDialog.CANCEL_OPTION // 2
StyledConfirmDialog.OK_OPTION     // 0
```

---

## ⌨️ Keyboard Shortcuts

- **ESC** - Close dialog (acts as Cancel)
- Dialog is **draggable** - click and drag anywhere

---

## 📝 Best Practices

✅ **DO**
- Use explicit type methods for important messages
- Provide clear, concise messages
- Use auto-detection for quick notifications
- Combine confirmation with success feedback

❌ **DON'T**
- Mix notification and dialog keywords
- Use toasts for critical errors (use dialogs)
- Show multiple toasts simultaneously
- Use overly long messages

---

## 🐛 Troubleshooting

### Toast not showing?
- Check if parent component is visible
- Verify message is not null
- Ensure Swing event thread

### Dialog appears behind window?
- Use correct parent component
- Check window modality

### Import errors?
- IDE module: `com.ing.ide.util.Notification`
- StoryWriter: `com.ing.storywriter.util.Notification`

---

## 📚 Full Documentation

See [TOAST_AND_DIALOG_MODERNIZATION.md](TOAST_AND_DIALOG_MODERNIZATION.md) for:
- Complete API reference
- Migration guide
- Customization options
- Advanced examples

---

*Quick reference for INGenious toast messages and confirmation dialogs*

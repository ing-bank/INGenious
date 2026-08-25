# Duplicate Action Names

## Symptom
```
Duplicate action 'Click' for object type 'PLAYWRIGHT' detected
```

## Cause
Your plugin defines an action with the same name as:
- A core framework action
- Another plugin's action
- Another action in the same plugin

Action names must be unique within each object type.

## Solution

Rename your action method with a unique, descriptive name:

```java
// ❌ Before (conflicts with core)
@Action(object = ObjectType.PLAYWRIGHT, desc = "Click on [<Object>]")
public void Click() {
    // ...
}

// ✅ After (unique name)
@Action(object = ObjectType.PLAYWRIGHT, desc = "Click with 2 second delay on [<Object>]")
public void ClickWithDelay() {
    Thread.sleep(2000);
    Locator.click();
}
```

## Naming Best Practices

**Be specific about what makes your action different:**
- `ClickWithDelay()` - adds waiting
- `ClickAndVerify()` - includes verification
- `DoubleClick()` - different interaction
- `RightClick()` - different mouse button

**Use descriptive suffixes:**
- `Custom` - e.g., `CustomClick()`
- `Advanced` - e.g., `AdvancedFill()`
- `Extended` - e.g., `ExtendedNavigate()`

## Checking for Conflicts

Before deploying, search for existing actions:
1. Check core framework actions in INGenious Studio
2. Review other installed plugins
3. Search your own plugin for duplicate method names

## Multiple Plugins

If you have multiple plugins, ensure unique action names across all of them or use different object types.

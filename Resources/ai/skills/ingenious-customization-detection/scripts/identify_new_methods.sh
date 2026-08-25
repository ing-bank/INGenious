#!/bin/bash
################################################################################
# identify_new_methods.sh
#
# Purpose: Identify new action methods added to customized INGenious
# Usage: ./identify_new_methods.sh <user_path> <official_path> <module>
# Example: ./identify_new_methods.sh /path/to/user /tmp/official Engine
#
# Detects:
# - New public methods that could be plugin actions
# - New classes (entire files added)
# - Filters out getters, setters, standard methods
################################################################################

USER_PATH="$1"
OFFICIAL_PATH="$2"
MODULE="${3:-Engine}"

if [ -z "$USER_PATH" ] || [ -z "$OFFICIAL_PATH" ]; then
    echo "Usage: $0 <user_path> <official_path> [module]"
    exit 1
fi

OUTPUT_FILE="new_methods_${MODULE}.txt"
NEW_CLASSES_FILE="new_classes_${MODULE}.txt"

echo "Identifying new methods and classes in $MODULE..."

# Find new classes (files that don't exist in official)
find "$USER_PATH/$MODULE/src" -name "*.java" 2>/dev/null | while read -r file; do
    relative_path="${file#$USER_PATH/}"
    if [ ! -f "$OFFICIAL_PATH/$relative_path" ]; then
        echo "NEW CLASS: $file" >> "$NEW_CLASSES_FILE"
    fi
done

# Find new methods in existing files using diff
diff -r "$OFFICIAL_PATH/$MODULE/src" "$USER_PATH/$MODULE/src" 2>/dev/null | \
    grep "^>" | \
    grep -E "public (void|boolean|String|int|Object)" | \
    grep -v "^>(get|set|is)" | \
    grep -v "^>(equals|hashCode|toString)" \
    > "$OUTPUT_FILE"

# Count results
new_classes=$(wc -l < "$NEW_CLASSES_FILE" 2>/dev/null || echo 0)
new_methods=$(wc -l < "$OUTPUT_FILE" 2>/dev/null || echo 0)

echo ""
echo "Detection Results:"
echo "  New classes found: $new_classes"
echo "  New methods found: $new_methods"
echo ""
echo "Results saved to:"
[ -f "$NEW_CLASSES_FILE" ] && echo "  - $NEW_CLASSES_FILE"
[ -f "$OUTPUT_FILE" ] && echo "  - $OUTPUT_FILE"

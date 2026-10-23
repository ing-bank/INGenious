#!/bin/bash
################################################################################
# generate_diff.sh
#
# Purpose: Generate detailed diff patches for customized modules
# Usage: ./generate_diff.sh <user_path> <official_path> <module_name>
# Example: ./generate_diff.sh /path/to/user /tmp/official Engine
#
# Generates unified diff with:
# - Excluded: target/, *.class, .git, .idea, *.iml
# - Statistics: lines added, removed, files changed
################################################################################

USER_PATH="$1"
OFFICIAL_PATH="$2"
MODULE="$3"

if [ -z "$USER_PATH" ] || [ -z "$OFFICIAL_PATH" ] || [ -z "$MODULE" ]; then
    echo "Usage: $0 <user_path> <official_path> <module_name>"
    exit 1
fi

OUTPUT_FILE="detailed_diff_${MODULE}.patch"

echo "Generating detailed diff for $MODULE..."

# Generate unified diff (ignores whitespace changes with -b)
diff -Naur -b \
    --exclude='target' \
    --exclude='*.class' \
    --exclude='.git' \
    --exclude='*.iml' \
    --exclude='.idea' \
    --exclude='node_modules' \
    --exclude='*.log' \
    "$OFFICIAL_PATH/$MODULE" \
    "$USER_PATH/$MODULE" \
    > "$OUTPUT_FILE"

# Calculate statistics
if [ -f "$OUTPUT_FILE" ]; then
    ADDED=$(grep -c "^+" "$OUTPUT_FILE" || echo 0)
    REMOVED=$(grep -c "^-" "$OUTPUT_FILE" || echo 0)
    FILES_CHANGED=$(grep -c "^diff" "$OUTPUT_FILE" || echo 0)
    
    echo ""
    echo "Diff Statistics for $MODULE:"
    echo "  Files changed: $FILES_CHANGED"
    echo "  Lines added: $ADDED"
    echo "  Lines removed: $REMOVED"
    echo ""
    echo "Diff saved to: $OUTPUT_FILE"
else
    echo "❌ Failed to generate diff"
    exit 1
fi

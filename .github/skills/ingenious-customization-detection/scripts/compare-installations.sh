#!/bin/bash
# Compare user's INGenious installation against official release
# Usage: ./compare-installations.sh <user-path> <official-path> <copy-type> <output-dir>

set -e

USER_PATH="$1"
OFFICIAL_PATH="$2"
COPY_TYPE="$3"
OUTPUT_DIR="$4"

# Validate input
if [[ -z "$USER_PATH" ]] || [[ -z "$OFFICIAL_PATH" ]] || [[ -z "$COPY_TYPE" ]] || [[ -z "$OUTPUT_DIR" ]]; then
    echo "ERROR: Missing required arguments" >&2
    echo "Usage: $0 <user-path> <official-path> <copy-type> <output-dir>" >&2
    echo "Copy type: SOURCE_CODE_COPY or BUILD_COPY" >&2
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Define modules to compare based on copy type
if [[ "$COPY_TYPE" == "SOURCE_CODE_COPY" ]]; then
    MODULES=("Common" "Datalib" "Engine" "IDE" "ingenious-api" "StoryWriter")
else
    MODULES=("Engine")
fi

echo "Comparing modules: ${MODULES[*]}"
echo ""

# Compare each module
for module in "${MODULES[@]}"; do
    if [[ ! -d "$USER_PATH/$module" ]]; then
        echo "⚠️  Skipping $module (not found in user installation)"
        continue
    fi
    
    if [[ ! -d "$OFFICIAL_PATH/$module" ]]; then
        echo "⚠️  Skipping $module (not found in official release)"
        continue
    fi
    
    echo "Comparing $module..."
    
    # Generate detailed diff
    diff -Naur \
        --exclude='target' \
        --exclude='*.class' \
        --exclude='.git' \
        --exclude='*.iml' \
        --exclude='.idea' \
        --exclude='node_modules' \
        --exclude='*.log' \
        --exclude='.DS_Store' \
        "$OFFICIAL_PATH/$module" \
        "$USER_PATH/$module" \
        > "$OUTPUT_DIR/diff_${module}.patch" 2>&1 || true
    
    # Generate quick summary
    diff -qr \
        --exclude='target' \
        --exclude='*.class' \
        --exclude='.git' \
        --exclude='*.iml' \
        --exclude='.idea' \
        "$OFFICIAL_PATH/$module" \
        "$USER_PATH/$module" \
        > "$OUTPUT_DIR/files_${module}.txt" 2>&1 || true
    
    # Count changes
    if [[ -f "$OUTPUT_DIR/diff_${module}.patch" ]]; then
        LINES_ADDED=$(grep -c "^+" "$OUTPUT_DIR/diff_${module}.patch" 2>/dev/null || echo "0")
        LINES_REMOVED=$(grep -c "^-" "$OUTPUT_DIR/diff_${module}.patch" 2>/dev/null || echo "0")
        FILES_CHANGED=$(grep -c "^diff" "$OUTPUT_DIR/diff_${module}.patch" 2>/dev/null || echo "0")
        
        echo "  Files changed: $FILES_CHANGED"
        echo "  Lines added: $LINES_ADDED"
        echo "  Lines removed: $LINES_REMOVED"
    fi
    
    echo ""
done

# Create summary
echo "Comparison complete!"
echo "Output files saved to: $OUTPUT_DIR"
echo ""
echo "Files created:"
ls -lh "$OUTPUT_DIR"

exit 0

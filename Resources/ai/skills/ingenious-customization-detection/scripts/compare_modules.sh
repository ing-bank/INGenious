#!/bin/bash
################################################################################
# compare_modules.sh
#
# Purpose: Compare user's INGenious modules against official release
# Usage: ./compare_modules.sh <user_path> <official_path> <copy_type>
# Example: ./compare_modules.sh /path/to/user /tmp/official SOURCE_CODE_COPY
#
# Copy Type: SOURCE_CODE_COPY or BUILD_COPY
# - SOURCE_CODE_COPY: Compares all modules
# - BUILD_COPY: Compares only Engine module
################################################################################

USER_PATH="$1"
OFFICIAL_PATH="$2"
COPY_TYPE="${3:-SOURCE_CODE_COPY}"

if [ -z "$USER_PATH" ] || [ -z "$OFFICIAL_PATH" ]; then
    echo "Usage: $0 <user_path> <official_path> [copy_type]"
    exit 1
fi

OUTPUT_DIR="./comparison_results"
mkdir -p "$OUTPUT_DIR"

# Define modules to compare
if [ "$COPY_TYPE" = "SOURCE_CODE_COPY" ]; then
    MODULES=(
        "Common"
        "Datalib"
        "Engine"
        "IDE"
        "ingenious-api"
        "StoryWriter"
        "TestData - Csv"
    )
else
    MODULES=("Engine")
fi

echo "Comparing modules: ${MODULES[*]}"
echo "Copy type: $COPY_TYPE"
echo ""

# Compare each module
for module in "${MODULES[@]}"; do
    if [ -d "$USER_PATH/$module" ] && [ -d "$OFFICIAL_PATH/$module" ]; then
        echo "Comparing $module..."
        
        # Quick comparison (files only, ignores whitespace changes with -b)
        diff -rq -b \
            --exclude='target' \
            --exclude='*.class' \
            --exclude='.git' \
            "$USER_PATH/$module/src" \
            "$OFFICIAL_PATH/$module/src" \
            > "$OUTPUT_DIR/changes_${module}.txt" 2>&1
        
        if [ $? -eq 0 ]; then
            echo "  ✓ No changes detected in $module"
        else
            changes=$(wc -l < "$OUTPUT_DIR/changes_${module}.txt")
            echo "  ⚠️  $changes differences found in $module"
        fi
    else
        echo "  ⚠️  Skipping $module (not found in both paths)"
    fi
done

echo ""
echo "Comparison complete. Results saved to: $OUTPUT_DIR/"

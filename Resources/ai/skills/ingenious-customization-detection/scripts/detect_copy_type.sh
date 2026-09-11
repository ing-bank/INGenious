#!/bin/bash
################################################################################
# detect_copy_type.sh
#
# Purpose: Determine if INGenious installation is source code or build copy
# Usage: ./detect_copy_type.sh <path_to_ingenious>
# Output: SOURCE_CODE_COPY, BUILD_COPY, or UNKNOWN
#
# Detection Logic:
# - Source Code Copy: Has multiple Maven modules (Datalib, Common, IDE, etc.)
# - Build Copy: Only has Engine module with source code
################################################################################

INGENIOUS_PATH="${1:-.}"

# Check for module directories
modules_found=()

for module in "Datalib" "Common" "IDE" "StoryWriter" "ingenious-api" "TestData - Csv"; do
    if [ -d "$INGENIOUS_PATH/$module" ]; then
        modules_found+=("$module")
    fi
done

# Determine copy type
if [ ${#modules_found[@]} -gt 1 ]; then
    echo "SOURCE_CODE_COPY"
    echo "# Modules found: ${modules_found[*]}" >&2
elif [ -d "$INGENIOUS_PATH/Engine/src" ] && [ ${#modules_found[@]} -eq 0 ]; then
    echo "BUILD_COPY"
    echo "# Only Engine module found" >&2
else
    echo "UNKNOWN"
    echo "# Unable to determine copy type" >&2
fi

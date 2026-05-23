#!/bin/bash
# Determine if INGenious installation is source code copy or build copy
# Usage: ./detect-copy-type.sh <ingenious-root-path>

set -e

ROOT_PATH="$1"

# Validate input
if [[ -z "$ROOT_PATH" ]]; then
    echo "ERROR: Provide INGenious root path" >&2
    echo "Usage: $0 <ingenious-root-path>" >&2
    exit 1
fi

if [[ ! -d "$ROOT_PATH" ]]; then
    echo "ERROR: Path does not exist: $ROOT_PATH" >&2
    exit 1
fi

# Check for module directories that indicate source code copy
MODULES=("Datalib" "Common" "IDE" "StoryWriter" "ingenious-api")
SOURCE_MODULES_FOUND=0

for module in "${MODULES[@]}"; do
    if [[ -d "$ROOT_PATH/$module" ]] && [[ -d "$ROOT_PATH/$module/src" ]]; then
        ((SOURCE_MODULES_FOUND++))
    fi
done

# Determine copy type
if [[ $SOURCE_MODULES_FOUND -ge 2 ]]; then
    echo "SOURCE_CODE_COPY"
    exit 0
elif [[ -d "$ROOT_PATH/Engine/src" ]] && [[ $SOURCE_MODULES_FOUND -eq 0 ]]; then
    echo "BUILD_COPY"
    exit 0
else
    echo "UNKNOWN"
    exit 1
fi

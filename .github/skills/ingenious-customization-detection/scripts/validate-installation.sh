#!/bin/bash
# Unified validation for INGenious installations
# Usage: ./validate-installation.sh --check <markers|version|modules> --path <path>

set -e

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --check)
            CHECK_TYPE="$2"
            shift 2
            ;;
        --path)
            TARGET_PATH="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1" >&2
            echo "Usage: $0 --check <markers|version|modules> --path <path>" >&2
            exit 1
            ;;
    esac
done

# Validate arguments
if [[ -z "$CHECK_TYPE" ]] || [[ -z "$TARGET_PATH" ]]; then
    echo "ERROR: Missing required arguments" >&2
    echo "Usage: $0 --check <markers|version|modules> --path <path>" >&2
    exit 1
fi

if [[ ! -d "$TARGET_PATH" ]]; then
    echo "ERROR: Path does not exist: $TARGET_PATH" >&2
    exit 1
fi

case "$CHECK_TYPE" in
    markers)
        # Check for INGenious markers
        INDICATORS=(
            "Configuration/conf.js"
            "Configuration/XPLOR_SETTINGS.json"
            "Engine/pom.xml"
            "Projects"
        )
        
        FOUND=0
        for indicator in "${INDICATORS[@]}"; do
            if [[ -e "$TARGET_PATH/$indicator" ]]; then
                ((FOUND++))
            fi
        done
        
        if [[ $FOUND -ge 2 ]]; then
            echo "VALID"
            exit 0
        else
            echo "INVALID: Only $FOUND INGenious markers found (need at least 2)"
            exit 1
        fi
        ;;
    
    version)
        # Verify version can be detected
        if [[ -f "$TARGET_PATH/Engine/pom.xml" ]] || \
           [[ -f "$TARGET_PATH/Configuration/package.properties" ]] || \
           [[ -d "$TARGET_PATH/.git" ]]; then
            echo "VALID"
            exit 0
        else
            echo "INVALID: No version sources found"
            exit 1
        fi
        ;;
    
    modules)
        # Validate module structure
        if [[ -d "$TARGET_PATH/Engine/src" ]]; then
            echo "VALID"
            exit 0
        else
            echo "INVALID: Engine module not found"
            exit 1
        fi
        ;;
    
    *)
        echo "ERROR: Unknown check type: $CHECK_TYPE" >&2
        echo "Valid types: markers, version, modules" >&2
        exit 1
        ;;
esac

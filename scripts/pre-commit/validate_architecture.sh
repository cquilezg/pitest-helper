#!/bin/bash

# Script to validate imports in Kotlin files
# Verifies that imports comply with the rules defined for each folder

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Violation counter
violations=0

# Function to extract the qualified name from a Kotlin file
get_qualified_name() {
    local file="$1"
    local package_name=""
    local class_name=""

    # Extract package declaration
    while IFS= read -r line; do
        if [[ $line =~ ^package[[:space:]]+([a-zA-Z0-9_.]+) ]]; then
            package_name="${BASH_REMATCH[1]}"
        fi

        # Extract class/object/interface name (simplified - takes the first declaration)
        if [[ $line =~ ^(class|object|interface|enum\ class|data\ class|sealed\ class)[[:space:]]+([a-zA-Z0-9_]+) ]]; then
            class_name="${BASH_REMATCH[2]}"
            break
        fi
    done < "$file"

    if [ -n "$package_name" ] && [ -n "$class_name" ]; then
        echo "${package_name}.${class_name}"
    elif [ -n "$package_name" ]; then
        echo "$package_name"
    else
        echo "unknown"
    fi
}

# Function to validate imports in a file
validate_imports() {
    local file="$1"
    local allowed_patterns="$2"
    local folder_name="$3"

    # Get qualified name once for this file
    local qualified_name=$(get_qualified_name "$file")

    # Read file line by line
    line_number=0
    while IFS= read -r line; do
        ((line_number++))

        # Check if the line is an import
        if [[ $line =~ ^import[[:space:]]+ ]]; then
            # Extract the package from the import (without the initial "import ")
            import_statement=$(echo "$line" | sed 's/^import[[:space:]]\+//' | sed 's/[[:space:]]*$//')

            # Check if the import matches any of the allowed patterns
            is_valid=false
            IFS='|' read -ra PATTERNS <<< "$allowed_patterns"
            for pattern in "${PATTERNS[@]}"; do
                if [[ $import_statement == $pattern* ]]; then
                    is_valid=true
                    break
                fi
            done

            # If not valid, report the violation
            if [ "$is_valid" = false ]; then
                local filename=$(basename "$file")
                echo -e "${RED}[VIOLATION]${NC} $qualified_name(${filename}:${line_number})"
                echo -e "  > $line"
                echo ""
                ((violations++))
            fi
        fi
    done < "$file"
}

# Base path
BASE_PATH="src/main/kotlin/com/cquilez/pitesthelper"

# Allowed imports for domain
DOMAIN_ALLOWED_IMPORTS=(
    "java."
    "com.cquilez.pitesthelper.domain."
    "kotlin."
)

# Allowed imports for application
APPLICATION_ALLOWED_IMPORTS=(
    "java."
    "com.cquilez.pitesthelper.domain."
    "com.cquilez.pitesthelper.application."
    "com.intellij.openapi.components.service"
    "com.intellij.openapi.project.Project"
    "kotlin."
)

# Function to join array elements with a delimiter
join_by() {
    local IFS="$1"
    shift
    echo "$*"
}

# Validate files in the domain folder
echo "[DOMAIN]"
DOMAIN_PATH="$BASE_PATH/domain"
if [ -d "$DOMAIN_PATH" ]; then
    DOMAIN_PATTERNS=$(join_by "|" "${DOMAIN_ALLOWED_IMPORTS[@]}")
    while IFS= read -r -d '' file; do
        validate_imports "$file" "$DOMAIN_PATTERNS" "domain"
    done < <(find "$DOMAIN_PATH" -type f -name "*.kt" -print0)
else
    echo -e "${YELLOW}Warning: Folder '$DOMAIN_PATH' not found${NC}"
    exit 1
fi

# Validate files in the application folder
echo "[APPLICATION]"
APPLICATION_PATH="$BASE_PATH/application"
if [ -d "$APPLICATION_PATH" ]; then
    APPLICATION_PATTERNS=$(join_by "|" "${APPLICATION_ALLOWED_IMPORTS[@]}")
    while IFS= read -r -d '' file; do
        validate_imports "$file" "$APPLICATION_PATTERNS" "application"
    done < <(find "$APPLICATION_PATH" -type f -name "*.kt" -print0)
else
    echo -e "${YELLOW}Warning: Folder '$APPLICATION_PATH' not found${NC}"
    exit 1
fi

# Show summary
echo "================================================"
if [ $violations -eq 0 ]; then
    echo -e "${GREEN}✓ No violations found${NC}"
    echo "Total violations: 0"
    exit 0
else
    echo -e "${RED}✗ Import violations found${NC}"
    echo "Total violations: $violations"
    exit 1
fi
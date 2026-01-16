#!/usr/bin/env bash
#
# Validates that Kotlin code does not use the !! (not-null assertion) operator.
# Using !! can lead to NullPointerExceptions and should be avoided in favor of
# safe calls (?.), elvis operator (?:), or proper null handling.
#
# Usage: ./validate_no_double_bang.sh [directory]
#   directory: Optional. Directory to search (default: src)
#
# Exit codes:
#   0 - No !! found
#   1 - !! found in Kotlin files
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Default search directory
SEARCH_DIR="${1:-src}"

# Script directory (for resolving relative paths)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Change to project root
cd "${PROJECT_ROOT}"

# Check if search directory exists
if [[ ! -d "${SEARCH_DIR}" ]]; then
    echo -e "${RED}Error: Directory '${SEARCH_DIR}' does not exist${NC}"
    exit 1
fi

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
        # Fallback: use filename without extension as class name
        local filename=$(basename "$file" .kt)
        echo "${package_name}.${filename}"
    else
        echo "unknown"
    fi
}

echo -e "${YELLOW}Checking for '!!' (not-null assertion) in Kotlin files...${NC}"
echo ""

# Find all occurrences of !! in Kotlin files (excluding test files)
# Using grep with:
#   -r: recursive
#   -n: show line numbers
#   -H: show filename
#   --include: only .kt files
#   --exclude-dir: skip test directories
#   Pattern: !! but not inside strings or comments (simplified check)
VIOLATIONS=$(grep -rn --include="*.kt" --exclude-dir="test" --exclude-dir="testData" --exclude-dir="*Test*" '!!' "${SEARCH_DIR}" 2>/dev/null || true)

if [[ -z "${VIOLATIONS}" ]]; then
    echo -e "${GREEN}✓ No '!!' operators found in Kotlin code${NC}"
    exit 0
fi

# Count violations
VIOLATION_COUNT=$(echo "${VIOLATIONS}" | wc -l)

echo -e "${RED}✗ Found ${VIOLATION_COUNT} occurrence(s) of '!!' in Kotlin code:${NC}"
echo ""

# Cache for qualified names (file -> qualified_name)
declare -A QUALIFIED_NAME_CACHE

echo "${VIOLATIONS}" | while IFS= read -r line; do
    # Format: filename:line_number:content
    FILE=$(echo "${line}" | cut -d: -f1)
    LINE_NUM=$(echo "${line}" | cut -d: -f2)
    CONTENT=$(echo "${line}" | cut -d: -f3-)
    FILENAME=$(basename "${FILE}")
    
    # Get qualified name (use cache if available)
    if [[ -z "${QUALIFIED_NAME_CACHE[$FILE]+x}" ]]; then
        QUALIFIED_NAME_CACHE[$FILE]=$(get_qualified_name "$FILE")
    fi
    QUALIFIED_NAME="${QUALIFIED_NAME_CACHE[$FILE]}"
    
    echo -e "${RED}[VIOLATION]${NC} ${QUALIFIED_NAME}(${FILENAME}:${LINE_NUM})"
    echo -e "  > ${CONTENT}"
    echo ""
done

echo -e "${RED}Please replace '!!' with safe alternatives:${NC}"
echo "  - Safe call operator: obj?.method()"
echo "  - Elvis operator: value ?: defaultValue"
echo "  - requireNotNull(): requireNotNull(value) { \"error message\" }"
echo "  - checkNotNull(): checkNotNull(value) { \"error message\" }"
echo "  - let with safe call: obj?.let { ... }"
echo ""

exit 1


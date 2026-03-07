#!/usr/bin/env bash
# AccessibilityLintTest.sh
# Runs Android lintDebug and checks for ContentDescription warnings/errors.
# Returns exit code 0 if no ContentDescription issues found; 1 otherwise.
# Usage: ./AccessibilityLintTest.sh

set -e

echo "Running ./gradlew lintDebug..."
./gradlew lintDebug 2>&1

REPORT="app/build/reports/lint-results-debug.xml"

if [ ! -f "$REPORT" ]; then
    echo "Lint report not found at $REPORT"
    exit 1
fi

# Count ContentDescription issues in lint report
CD_COUNT=$(grep -c "ContentDescription\|ImageContentDescription" "$REPORT" 2>/dev/null || echo "0")

echo "ContentDescription issues found: $CD_COUNT"

if [ "$CD_COUNT" -gt 0 ]; then
    echo "FAIL: $CD_COUNT ContentDescription lint issues remain."
    grep "ContentDescription\|ImageContentDescription" "$REPORT"
    exit 1
fi

echo "PASS: No ContentDescription lint issues."
exit 0

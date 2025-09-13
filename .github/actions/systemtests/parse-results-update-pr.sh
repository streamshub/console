#!/usr/bin/env bash
set -xeuo pipefail

# Get directory of this script, regardless of where it was called from
SCRIPT_DIR="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

source $SCRIPT_DIR/common.sh

ROOT_DIR="$SCRIPT_DIR/../../.."
RESULT_DIR=${1:-"$ROOT_DIR/systemtests/target/failsafe-reports"}
RESULT_MD=${2:-"$ROOT_DIR/test-results.md"}

TOTAL=0
PASSED=0
FAILED=0
ERRORS=0
SKIPPED=0
FAILED_TESTS=""

echo "Prepare test results and update PR status"

TEST_FILES=()
if [[ -d "$RESULT_DIR" ]]; then
  echo "Directory exists. Finding TEST-*.xml files"
  # Collect files into an array
  while IFS= read -r -d '' f; do
    TEST_FILES+=("$f");
  done < <(find "$RESULT_DIR" -type f -name 'TEST-*.xml' -print0)

  echo "Found ${#TEST_FILES[@]} results files:"

  for f in "${TEST_FILES[@]}"; do
    echo "Found results file: $f";
  done
else
  echo "No test results directory found: $RESULT_DIR"
fi

for f in "${TEST_FILES[@]}"; do
  echo "Processing results file $f"

  # Get test type count directly from root testSuite tag
  TOTAL=$(yq -p=xml -o=json '.testsuite."+@tests"' "$f" | jq -r)
  FAILED=$(yq -p=xml -o=json '.testsuite."+@failures"' "$f" | jq -r)
  ERRORS=$(yq -p=xml -o=json '.testsuite."+@errors"' "$f" | jq -r)
  SKIPPED=$(yq -p=xml -o=json '.testsuite."+@skipped"' "$f" | jq -r)

  # Get more info about what test failed from list of testcases
  TESTCASES_JSON=$(yq -p=xml -o=json '.testsuite.testcase
    | (select(tag == "!!map") | [.]) + (select(tag == "!!seq"))
    | map({"classname": .["+@classname"], "name": .["+@name"], "failure": .failure, "error": .error})' "$f")
  echo "TestCases from the run in JSON $TESTCASES_JSON"

  TESTCASES=()
  if [[ -n "$TESTCASES_JSON" ]]; then
    mapfile -t TESTCASES < <(echo "$TESTCASES_JSON" | jq -c '.[]')
    echo "TestCases from the run $TESTCASES"
  else
    echo "Test cases were not found in the XML"
  fi

  for testcase in "${TESTCASES[@]}"; do
    CLASSNAME=$(echo "$testcase" | jq -r '.classname // "UNKNOWN_CLASS"')
    NAME=$(echo "$testcase" | jq -r '.name // ""')
    TEST_DISPLAY=$CLASSNAME

    if [[ -n "$NAME" ]]; then
      TEST_DISPLAY="$CLASSNAME#$NAME"
    fi

    echo "Checking test $TEST_DISPLAY for status"

    if echo "$testcase" | jq -e '.error != null or .failure != null' >/dev/null; then
      FAILED_TESTS+="$TEST_DISPLAY,"
    fi
  done
done

FAILED_TESTS="${FAILED_TESTS%,}"
PASSED=$((TOTAL - FAILED - ERRORS - SKIPPED))

echo "===> Summary: TOTAL=$TOTAL PASSED=$PASSED FAILED=$FAILED ERRORS=$ERRORS SKIPPED=$SKIPPED"
echo "===> Failed tests: $FAILED_TESTS"

# Determine overall status and symbol
if [[ $TOTAL -eq 0 ]]; then
  STATE="failure"
  STATUS_SYMBOL="⚠️"
  DESCRIPTION="No tests were executed"
elif [[ $((FAILED + ERRORS)) -eq 0 ]]; then
  STATE="success"
  STATUS_SYMBOL="✅"
  DESCRIPTION="Systemtests succeeded"
else
  STATE="failure"
  STATUS_SYMBOL="❌"
  DESCRIPTION="Systemtests failed"
fi

# Prepare list of failed testCase names for markdown
LIST_FAILED=""
if [[ -n "$FAILED_TESTS" ]]; then
  LIST_FAILED="#### Test Failures:"
  IFS=',' read -ra FAILED_ARRAY <<< "$FAILED_TESTS"
  for t in "${FAILED_ARRAY[@]}"; do
    [[ -n "$t" ]] && LIST_FAILED+="\n- $t"
  done
fi

# Write results markdown for PR comment
mkdir -p "$(dirname "$RESULT_MD")"
cat > $RESULT_MD <<- RESULTS
## $STATUS_SYMBOL Systemtests run finished - $STATE $STATUS_SYMBOL
### Test Summary:
- **TOTAL**: $TOTAL
- **PASS**: $PASSED
- **FAIL**: $((FAILED+ERRORS))
- **SKIP**: $SKIPPED

#### Used parameters:
* TEST_CASE: $TEST_CASE
* PROFILE: $PROFILE
* INSTALL_TYPE: $INSTALL_TYPE
* RETRY_COUNT: $RETRY_COUNT
* ENVS: $ENVS

$LIST_FAILED
RESULTS

echo "Results file $(cat $RESULT_MD)"

# Set status check of the PR
gh api repos/$REPO/statuses/$COMMIT_SHA -f state="$STATE" -f context="System Tests" -f description="$DESCRIPTION" -f target_url="$RUN_URL"

# Comment PR with results markdown - do not edit help comment
COMMENT_MODE=$(getEditModeOrSkipIfLastCommentIsHelp)
echo "Comment mode: $COMMENT_MODE"
gh pr comment $PR_NUMBER --repo $REPO $COMMENT_MODE --body-file $RESULT_MD

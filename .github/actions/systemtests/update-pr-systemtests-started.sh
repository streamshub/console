#!/usr/bin/env bash
set -xeuo pipefail

# Get directory of this script
SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

source $SCRIPT_DIR/common.sh

ROOT_DIR="$SCRIPT_DIR/../../.."
PARAMS_MD=${1:-"$ROOT_DIR/params.md"}

echo "Update PR status. Systemtests started"

# Write params.md
cat > $PARAMS_MD <<- PARAMS
## 🏃 Systemtests run started️ 🏃
Build phase succeeded. Triggering systemtests.
#### Used parameters
* TEST_CASE: ${TEST_CASE:-}
* PROFILE: ${PROFILE:-}
* INSTALL_TYPE: ${INSTALL_TYPE:-}
* RETRY_COUNT: ${RETRY_COUNT:-}
* ENVS: ${ENVS:-}
PARAMS

# Set status check
gh api repos/$REPO/statuses/$COMMIT_SHA \
  -f state="pending" -f context="System Tests" -f description="System tests are running..."

# Update PR comment
deleteLastStatusComment
gh pr comment $PR_NUMBER --repo $REPO --body-file $PARAMS_MD

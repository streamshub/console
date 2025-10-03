#!/usr/bin/env bash
set -xeuo pipefail

# Get directory of this script, regardless of where it was called from
SCRIPT_DIR="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

source $SCRIPT_DIR/common.sh

WORKFLOW_FILE=${1:-integration.yml}

echo "Check workflow run succeeded"

# Get branch from PR
BRANCH=$(gh pr view "$PR_NUMBER" --repo "$REPO" --json headRefName -q '.headRefName')
echo "Fetching latest Build workflow run for branch $BRANCH..."

# Fetch latest workflow run conclusion
BUILD_RUN=$(gh api "repos/$REPO/actions/workflows/$WORKFLOW_FILE/runs?branch=$BRANCH&per_page=1" \
  --jq '.workflow_runs[0].conclusion')

echo "Build workflow latest run conclusion: $BUILD_RUN"
if [[ "$BUILD_RUN" != "success" ]]; then
  # Do not edit help comment
  COMMENT_MODE=$(getEditModeOrSkipIfLastCommentIsHelp)
  echo "Comment mode: $COMMENT_MODE"
  gh pr comment $PR_NUMBER --repo $REPO $COMMENT_MODE --body "❌ Build did not succeed. Cannot trigger systemtests"
  exit 1
fi

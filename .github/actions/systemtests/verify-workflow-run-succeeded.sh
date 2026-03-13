#!/usr/bin/env bash
set -xeuo pipefail

# Get directory of this script, regardless of where it was called from
SCRIPT_DIR="$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

source $SCRIPT_DIR/common.sh

WORKFLOW_FILE=${1:-integration.yml}

echo "Check build-images job succeeded"

# Get branch from PR
BRANCH=$(gh pr view "$PR_NUMBER" --repo "$REPO" --json headRefName -q '.headRefName')
echo "Fetching latest Build workflow run for branch $BRANCH..."

# Get the latest run ID for the workflow
RUN_ID=$(gh api "repos/$REPO/actions/workflows/$WORKFLOW_FILE/runs?branch=$BRANCH&per_page=1" \
  --jq '.workflow_runs[0].id')

echo "Latest workflow run ID: $RUN_ID"

if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
  deleteLastStatusComment
  gh pr comment $PR_NUMBER --repo $REPO --body "❌ No workflow run found for branch $BRANCH. Cannot trigger systemtests"
  exit 1
fi

# Check specifically the build-images job conclusion within that run
BUILD_IMAGES_STATUS=$(gh api "repos/$REPO/actions/runs/$RUN_ID/jobs" \
  --jq '.jobs[] | select(.name=="build-images") | .conclusion')

echo "build-images job conclusion: $BUILD_IMAGES_STATUS"

if [[ "$BUILD_IMAGES_STATUS" != "success" ]]; then
  deleteLastStatusComment
  gh pr comment $PR_NUMBER --repo $REPO --body "❌ build-images job did not succeed (status: $BUILD_IMAGES_STATUS). Cannot trigger systemtests"
  exit 1
fi

# Export run ID so systemtests workflow can download artifacts from it directly
echo "ARTIFACTS_RUN_ID=$RUN_ID" >> $GITHUB_ENV
echo "Build images succeeded, artifacts available from run $RUN_ID"
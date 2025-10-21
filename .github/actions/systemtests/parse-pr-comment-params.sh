#!/usr/bin/env bash
set -xeuo pipefail

echo "Parse PR comment parameters from message $MESSAGE_BODY"

# Extract optional flags (sed deals with potential format of --flag=value and --flag value)
testcase=$(echo "$MESSAGE_BODY" | sed -nE 's/.*--testcase[= ]+([^[:space:]]+).*/\1/p')
profile=$(echo "$MESSAGE_BODY" | sed -nE 's/.*--profile[= ]+([^[:space:]]+).*/\1/p')
env_str=$(echo "$MESSAGE_BODY" | sed -nE 's/.*--env[= ]+([^[:space:]]+).*/\1/p')

install_type=$(echo "$MESSAGE_BODY" | sed -nE 's/.*--install-type[= ]+([^[:space:]]+).*/\1/p' | tr '[:upper:]' '[:lower:]')
install_type="${install_type:-olm}"
if [[ "$install_type" != "olm" && "$install_type" != "yaml" ]]; then
  echo "❌ Invalid --install-type value: $install_type. Must be 'olm' or 'yaml'."
  exit 1
fi

retry_count=$(echo "$MESSAGE_BODY" | sed -nE 's/.*--retry-count[= ]+([^[:space:]]+).*/\1/p')
retry_count="${retry_count:-0}"
if [[ ! "$retry_count" =~ ^[0-9]+$ ]]; then
  echo "❌ Invalid --retry-count value: $retry_count. Must be a positive integer."
  exit 1
fi

# Export as step outputs
echo "TESTCASE=$testcase" >> $GITHUB_ENV
echo "PROFILE=$profile" >> $GITHUB_ENV
echo "ENVS=$env_str" >> $GITHUB_ENV
echo "INSTALL_TYPE=$install_type" >> $GITHUB_ENV
echo "RETRY_COUNT=$retry_count" >> $GITHUB_ENV

# Export current PR branch latest commit sha
commit_sha=$(gh pr view $PR_NUMBER --json headRefOid -q '.headRefOid')
echo "COMMIT_SHA=$commit_sha" >> $GITHUB_ENV

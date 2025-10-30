#!/usr/bin/env bash
set -xeuo pipefail

echo "Prepare systemtests environment variables"

# Export each key=value pair in ENVS
IFS=';' read -ra KV_PAIRS <<< "$ENVS"
for KV in "${KV_PAIRS[@]}"; do
  echo "$KV" >> "$GITHUB_ENV"
done

# Build Maven parameters
PARAMETERS="-Dfailsafe.rerunFailingTestsCount=${RETRY_COUNT}"

if [[ -n "$TEST_CASE" ]]; then
  PARAMETERS="$PARAMETERS -Dit.test=${TEST_CASE} -DskipSTs=false"
fi

if [[ -n "$PROFILE" ]]; then
  PARAMETERS="$PARAMETERS -P $PROFILE"
fi

# Export parameters
echo "PARAMETERS=$PARAMETERS" >> "$GITHUB_ENV"

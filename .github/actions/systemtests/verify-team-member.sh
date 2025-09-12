#!/usr/bin/env bash
set -xeuo pipefail

ORG=$1
TEAM=$2
COMMENTER=$3

echo "Verify that user is a team member"

echo "Fetching team members for $ORG/$TEAM..."
members=$(gh api "orgs/$ORG/teams/$TEAM/members" --jq '.[].login' --paginate)

if [[ -z "$members" ]]; then
  echo "❌ Unable to get team members"
  exit 1
fi

echo "Checking if $COMMENTER is in the team..."

is_member=false
while IFS= read -r member; do
  if [[ "$member" == "$COMMENTER" ]]; then
    is_member=true
    break
  fi
done <<< "$members"

if $is_member; then
  echo "✅ $COMMENTER IS a member of $ORG/$TEAM"
else
  echo "❌ $COMMENTER is not allowed to trigger this workflow"
  exit 1
fi

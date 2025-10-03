#!/usr/bin/env bash
set -xeuo pipefail

# Returns empty edit mode if the last bot comment is the help message
getEditModeOrSkipIfLastCommentIsHelp() {
  local marker="ℹ️ Systemtests Help ℹ️"
  local comment_author=$(gh api user --jq '.login')

  local last_body=$(gh pr view "$PR_NUMBER" --repo "$REPO" \
    --json comments \
    --jq '.comments
          | map(select(.author.login=="'"$comment_author"'"))
          | sort_by(.updatedAt)
          | last
          | .body // empty')

  if [[ -n $last_body && $last_body != *"$marker"* ]]; then
    echo "--edit-last"
  else
    echo ""
  fi
}

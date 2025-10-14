#!/usr/bin/env bash
set -xeuo pipefail

# Deletes the last non-help bot comment if it exists
deleteLastStatusComment() {
  local marker="ℹ️ Systemtests Help ℹ️"
  local comment_author
  comment_author=$(gh api user --jq '.login')

  # Get the last comment id and body by this bot
  local last_comment_info
  last_comment_info=$(gh api --paginate "/repos/$REPO/issues/$PR_NUMBER/comments" \
    --jq 'map(select(.user.login=="'"$comment_author"'")) | sort_by(.updated_at) | last')

  # Exit early if there’s no comment
  [[ -z "$last_comment_info" || "$last_comment_info" == "null" ]] && return 0

  local last_body
  last_body=$(jq -r '.body // empty' <<<"$last_comment_info")
  local last_id
  last_id=$(jq -r '.id // empty' <<<"$last_comment_info")

  # If it’s not a help comment, delete it
  if [[ -n "$last_id" && "$last_body" != *"$marker"* ]]; then
    echo "🗑️ Deleting last non-help comment (id=$last_id)"
    gh api -X DELETE "/repos/$REPO/issues/comments/$last_id"
  else
    echo "ℹ️ Last comment is not status comment — keep it."
  fi
}
export function encodeGroupId(groupId: string) {
  return groupId === "" ? "+" : encodeURIComponent(groupId)
}
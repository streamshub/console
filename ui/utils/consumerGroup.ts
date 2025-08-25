export function encodeGroupId(groupId: string): string {
  return groupId === "" ? "+" : encodeURIComponent(groupId).replace(/\./g, "%2E");
}

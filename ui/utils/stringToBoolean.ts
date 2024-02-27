export function stringToBoolean(value: string | undefined) {
  if (value === undefined) {
    return undefined;
  }
  try {
    const maybeBoolean = JSON.parse(value);
    return typeof maybeBoolean === "boolean" ? maybeBoolean : undefined;
  } catch {}
  return undefined;
}

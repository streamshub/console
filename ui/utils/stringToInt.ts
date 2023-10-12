export function stringToInt(value: string | undefined) {
  if (value === undefined) {
    return undefined;
  }
  const maybeNumber = parseInt(value, 10);
  return Number.isInteger(maybeNumber) ? maybeNumber : undefined;
}

import { Message } from "../_api/types";

export function isSameMessage(m1: Message, m2: Message) {
  return JSON.stringify(m1) === JSON.stringify(m2);
}

export function beautifyUnknownValue(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch (e) {
    // noop
  }
  return value;
}

export function truncate(value: string, length: number): [string, boolean] {
  const shouldTruncate = value.length > length;
  return [
    shouldTruncate ? `${value.substring(0, length)}...` : value,
    shouldTruncate,
  ];
}

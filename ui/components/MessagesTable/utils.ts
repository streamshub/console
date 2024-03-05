import { Message } from "@/api/messages/schema";

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

export function maybeJson(value: string): [string | object, boolean] {
  try {
    const parsed = JSON.parse(value);
    return [parsed, typeof parsed !== "string"];
  } catch (e) {
    // noop
  }
  return [value, false];
}

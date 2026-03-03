import { ConsumerGroup } from "@/api/groups/schema";

export function describeEnabled(group: ConsumerGroup): boolean {
  if (group.attributes.type) {
    switch (group.attributes.type) {
      case "Classic":
      case "Consumer":
      case "Share":
      case "Streams":
        return true;
      default:
        return false;
    }
  }

  return false;
}

export function resetMenuDisabled(group: ConsumerGroup): boolean {
  if (!describeEnabled(group)) {
    return true;
  }

  switch (group.attributes.type) {
    case "Classic":
    case "Consumer":
      return group.attributes.protocol !== "consumer";
    default:
      return true;
  }
}

export function resetButtonDisabled(group: ConsumerGroup): boolean {
  if (group.attributes.type) {
    switch (group.attributes.type) {
      case "Classic":
      case "Consumer":
        return group.attributes.state !== "EMPTY" || group.attributes.protocol !== "consumer";
      case "Share":
      case "Streams":
      default:
        return true;
    }
  }

  return true;
}

import { TopicMutateError } from "@/api/topics/schema";

export function topicMutateErrorToFieldError(
  error: TopicMutateError | "unknown" | undefined,
  isConfig: boolean,
  fields: string[],
) {
  if (error && error !== "unknown" && error.errors.length > 0) {
    const field = error.errors[0].source?.pointer?.split("/")[isConfig ? 4 : 3];
    if (field && fields.includes(field)) {
      return {
        field,
        error: error.errors[0].detail,
      };
    }
  }
  return undefined;
}

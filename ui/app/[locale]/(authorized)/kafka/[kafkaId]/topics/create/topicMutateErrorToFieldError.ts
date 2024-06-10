import { ApiError } from "@/api/api";

export function topicMutateErrorToFieldError(
  errors: ApiError[] | undefined,
  isConfig: boolean,
  fields: string[],
) {
  if (errors) {
    const fieldErrors = errors.map(error => {
      const field = error.source?.pointer?.split("/")[isConfig ? 4 : 3];
      if (field && fields.includes(field)) {
        return {
          field,
          error: error.detail,
        };
      }
    });

    return fieldErrors.length > 0 ? fieldErrors[0] : undefined
  }
  return undefined;
}

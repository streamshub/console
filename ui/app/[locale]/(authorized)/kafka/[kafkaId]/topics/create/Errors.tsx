import { ApiError } from "@/api/api";
import { Alert, Text, TextContent } from "@patternfly/react-core";

export function Errors({ errors }: { errors: ApiError[] | undefined }) {
  return errors !== undefined ? (
    errors.map((e, idx) => (
      <Alert key={idx} title={e.title} variant={"danger"}>
        <TextContent>
          <Text>{e.detail}</Text>
          {e.source?.pointer && (
            <Text component={"small"}>
              <strong>Pointer</strong>&nbsp;
              {e.source.pointer}
            </Text>
          )}

          <Text component={"small"}>
            <strong>Error</strong>&nbsp;
            {e.id}
          </Text>
        </TextContent>
      </Alert>
    ))
  ) : (
    <Alert title={"Unexpected error"} variant={"danger"}>
      Sorry, something went wrong. Please try again later.
    </Alert>
  );
}

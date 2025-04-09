import { ApiError } from "@/api/api";
import { Alert, Content } from "@patternfly/react-core";

export function Errors({ errors }: { errors: ApiError[] | undefined }) {
  return errors !== undefined ? (
    errors.map((e, idx) => (
      <Alert key={idx} title={e.title} variant={"danger"}>
        <Content>
          <Content component="p">{e.detail}</Content>
          {e.source?.pointer && (
            <Content component={"small"}>
              <strong>Pointer</strong>&nbsp;
              {e.source.pointer}
            </Content>
          )}

          <Content component={"small"}>
            <strong>Error</strong>&nbsp;
            {e.id}
          </Content>
        </Content>
      </Alert>
    ))
  ) : (
    <Alert title={"Unexpected error"} variant={"danger"}>
      Sorry, something went wrong. Please try again later.
    </Alert>
  );
}

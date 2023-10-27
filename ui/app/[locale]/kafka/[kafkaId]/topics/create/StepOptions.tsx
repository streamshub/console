import { ConfigMap, NewConfigMap, TopicMutateError } from "@/api/topics";
import { ConfigTable } from "@/app/[locale]/kafka/[kafkaId]/topics/create/ConfigTable";
import { Error } from "@/app/[locale]/kafka/[kafkaId]/topics/create/Errors";
import { topicMutateErrorToFieldError } from "@/app/[locale]/kafka/[kafkaId]/topics/create/topicMutateErrorToFieldError";
import { Text, TextContent, Title } from "@patternfly/react-core";

export function StepOptions({
  options,
  initialOptions,
  onChange,
  error,
}: {
  options: NewConfigMap;
  initialOptions: Readonly<ConfigMap>;
  onChange: (options: NewConfigMap) => void;
  error: TopicMutateError | "unknown" | undefined;
}) {
  const fieldError = topicMutateErrorToFieldError(
    error,
    true,
    Object.keys(initialOptions),
  );
  return (
    <>
      {error && !fieldError && <Error error={error} />}
      <TextContent>
        <Title headingLevel={"h2"}>Options</Title>
        <Text>Configure other topic configuration options</Text>
      </TextContent>
      <ConfigTable
        options={options}
        initialOptions={initialOptions}
        onChange={onChange}
        fieldError={fieldError}
      />
    </>
  );
}

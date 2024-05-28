import { ConfigMap, NewConfigMap, TopicMutateError } from "@/api/topics/schema";
import { ConfigTable } from "@/app/[locale]/kafka/[kafkaId]/topics/create/ConfigTable";
import { Error } from "@/app/[locale]/kafka/[kafkaId]/topics/create/Errors";
import { topicMutateErrorToFieldError } from "@/app/[locale]/kafka/[kafkaId]/topics/create/topicMutateErrorToFieldError";
import { Text, TextContent, Title } from "@patternfly/react-core";
import { useTranslations } from "next-intl";

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
  const t = useTranslations();
  const fieldError = topicMutateErrorToFieldError(
    error,
    true,
    Object.keys(initialOptions),
  );
  return (
    <>
      {error && !fieldError && <Error error={error} />}
      <TextContent>
        <Title headingLevel={"h2"}>{t("CreateTopic.step_option_title")}</Title>
        <Text>{t("CreateTopic.step_option_description")}</Text>
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

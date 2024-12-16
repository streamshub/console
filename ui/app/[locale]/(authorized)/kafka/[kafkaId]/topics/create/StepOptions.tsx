import { ApiError } from "@/api/api";
import { ConfigMap, NewConfigMap } from "@/api/topics/schema";
import { ConfigTable } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/ConfigTable";
import { Errors } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/Errors";
import { topicMutateErrorToFieldError } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/topicMutateErrorToFieldError";
import { Text, TextContent, Title } from "@patternfly/react-core";
import { useTranslations } from "next-intl";

export function StepOptions({
  options,
  initialOptions,
  onChange,
  errors,
}: {
  options: NewConfigMap;
  initialOptions: Readonly<ConfigMap>;
  onChange: (options: NewConfigMap) => void;
  errors: ApiError[] | undefined;
}) {
  const t = useTranslations();
  const fieldError = topicMutateErrorToFieldError(
    errors,
    true,
    Object.keys(initialOptions),
  );
  return (
    <>
      {errors && !fieldError && <Errors errors={errors} />}
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

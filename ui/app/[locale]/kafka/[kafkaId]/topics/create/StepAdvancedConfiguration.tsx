import { ConfigSchemaMap } from "@/api/topics";
import { ConfigTable } from "@/app/[locale]/kafka/[kafkaId]/topics/create/ConfigTable";
import { Text, TextContent, Title } from "@patternfly/react-core";

export function StepAdvancedConfiguration({
  options,
  defaultOptions,
  onChange,
  showErrors,
}: {
  options: ConfigSchemaMap;
  defaultOptions: Readonly<ConfigSchemaMap>;
  onChange: (options: number) => void;
  showErrors: boolean;
}) {
  return (
    <>
      <TextContent className={"pf-v5-u-pb-md"}>
        <Title headingLevel={"h2"}>Advanced configuration</Title>
        <Text>Configure other topic configuration options</Text>
      </TextContent>
      <ConfigTable options={options} defaultOptions={defaultOptions} />
    </>
  );
}

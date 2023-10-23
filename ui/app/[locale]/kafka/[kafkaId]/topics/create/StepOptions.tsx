import { ConfigMap, NewConfigMap } from "@/api/topics";
import { ConfigTable } from "@/app/[locale]/kafka/[kafkaId]/topics/create/ConfigTable";
import { Text, TextContent, Title } from "@patternfly/react-core";

export function StepOptions({
  options,
  initialOptions,
  onChange,
}: {
  options: NewConfigMap;
  initialOptions: Readonly<ConfigMap>;
  onChange: (options: NewConfigMap) => void;
}) {
  return (
    <>
      <TextContent>
        <Title headingLevel={"h2"}>Options</Title>
        <Text>Configure other topic configuration options</Text>
      </TextContent>
      <ConfigTable
        options={options}
        initialOptions={initialOptions}
        onChange={onChange}
      />
    </>
  );
}

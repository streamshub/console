import {
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  Text,
  TextContent,
  TextInput,
  Title,
} from "@patternfly/react-core";

export function StepName({
  name,
  onChange,
  invalid,
}: {
  name: string;
  onChange: (name: string) => void;
  invalid: boolean;
}) {
  return (
    <>
      <TextContent className={"pf-v5-u-pb-md"}>
        <Title headingLevel={"h2"}>Topic name</Title>
        <Text>Unique name used to recognize your topic</Text>
        <Text component={"small"}>
          The topic name is also used by your producers and consumers as part of
          the connection information, so make it something easy to recognize.
        </Text>
      </TextContent>
      <FormGroup label="Topic name" isRequired fieldId="topic-name">
        <TextInput
          isRequired
          type="text"
          id="topic-name"
          name="topic-name"
          aria-describedby="topic-name-helper"
          value={name}
          onChange={(_, value) => onChange(value)}
          validated={invalid ? "error" : "default"}
        />
        <FormHelperText>
          <HelperText>
            <HelperTextItem>
              Must be letters (Aa-Zz), numbers, underscores ( _ ), periods ( .
              ), or hyphens ( - )
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      </FormGroup>
    </>
  );
}

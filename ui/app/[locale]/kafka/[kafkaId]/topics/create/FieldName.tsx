import {
  FormGroup,
  FormHelperText,
  FormSection,
  HelperText,
  HelperTextItem,
  Text,
  TextContent,
  TextInput,
  Title,
} from "@patternfly/react-core";

export function FieldName({
  name,
  onChange,
  nameInvalid,
  lengthInvalid,
  formatInvalid,
  backendError,
}: {
  name: string;
  onChange: (name: string) => void;
  nameInvalid: boolean;
  lengthInvalid: boolean;
  formatInvalid: boolean;
  backendError: string | false;
}) {
  const showErrors = name !== "";
  return (
    <FormSection>
      <TextContent>
        <Title headingLevel={"h3"}>Topic name</Title>
        <Text component={"small"}>
          Unique name used to recognize your topic. The topic name is also used
          by your producers and consumers as part of the connection information,
          so make it something easy to recognize.
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
          validated={
            showErrors &&
            (nameInvalid || lengthInvalid || formatInvalid || backendError)
              ? "error"
              : "default"
          }
        />
        <FormHelperText>
          <HelperText component="ul" aria-live="polite" id="topic-name-helper">
            <HelperTextItem
              isDynamic
              variant={showErrors && lengthInvalid ? "error" : "indeterminate"}
              component="li"
            >
              Must be at least 3 characters
            </HelperTextItem>
            <HelperTextItem
              isDynamic
              variant={showErrors && nameInvalid ? "error" : "indeterminate"}
              component="li"
            >
              Cannot be &quot;.&quot; or &quot;..&quot;
            </HelperTextItem>
            <HelperTextItem
              isDynamic
              variant={showErrors && formatInvalid ? "error" : "indeterminate"}
              component={"li"}
            >
              Must be letters (Aa-Zz), numbers, underscores ( _ ), periods ( .
              ), or hyphens ( - )
            </HelperTextItem>
            {backendError && (
              <HelperTextItem isDynamic variant={"error"} component={"li"}>
                {backendError}
              </HelperTextItem>
            )}
          </HelperText>
        </FormHelperText>
      </FormGroup>
    </FormSection>
  );
}

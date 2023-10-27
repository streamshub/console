import {
  FormGroup,
  FormHelperText,
  FormSection,
  HelperText,
  HelperTextItem,
  NumberInput,
  Text,
  TextContent,
  Title,
} from "@patternfly/react-core";

export function FieldPartitions({
  partitions,
  onChange,
  invalid,
  backendError,
}: {
  partitions: number;
  onChange: (partitions: number) => void;
  invalid: boolean;
  backendError: string | false;
}) {
  return (
    <FormSection>
      <TextContent>
        <Title headingLevel={"h3"}>Partitions</Title>
        <Text component={"small"}>
          An ordered list of messages. One or more partitions make up a topic.
          Partitions are distributed across the brokers to increase the
          scalability of your topic. You can also use them to distribute
          messages across the members of the consumer group.
        </Text>
      </TextContent>
      <FormGroup label="Partitions" isRequired fieldId="topic-partitions">
        <NumberInput
          required
          id="topic-partitions"
          name="topic-partitions"
          aria-describedby="topic-partitions-helper"
          value={partitions}
          onChange={(ev) =>
            onChange(parseInt((ev.target as HTMLInputElement).value, 10))
          }
          onMinus={() => onChange(partitions > 1 ? partitions - 1 : partitions)}
          onPlus={() => onChange(partitions + 1)}
          min={1}
          validated={invalid || backendError ? "error" : "default"}
        />
        <FormHelperText>
          <HelperText id={"topic-partitions-helper"}>
            <HelperTextItem variant={"indeterminate"}>
              One partition is sufficient for getting started, but production
              systems often have more.{" "}
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

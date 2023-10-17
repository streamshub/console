import {
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  NumberInput,
  Text,
  TextContent,
  Title,
} from "@patternfly/react-core";

export function StepPartitions({
  partitions,
  onChange,
  invalid,
}: {
  partitions: number;
  onChange: (partitions: number) => void;
  invalid: boolean;
}) {
  return (
    <>
      <TextContent className={"pf-v5-u-pb-md"}>
        <Title headingLevel={"h2"}>Partitions</Title>
        <Text>An ordered list of messages</Text>
        <Text component={"small"}>
          One or more partitions make up a topic. Partitions are distributed
          across the brokers to increase the scalability of your topic. You can
          also use them to distribute messages across the members of the
          consumer group.
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
          validated={invalid ? "error" : "default"}
        />
        <FormHelperText>
          <HelperText>
            <HelperTextItem>
              One partition is sufficient for getting started, but production
              systems often have more.{" "}
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      </FormGroup>
    </>
  );
}

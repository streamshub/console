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

export function StepReplicas({
  replicas,
  maxReplicas,
  onChange,
  showErrors,
}: {
  replicas: number;
  maxReplicas: number;
  onChange: (replicas: number) => void;
  showErrors: boolean;
}) {
  return (
    <FormSection>
      <TextContent>
        <Title headingLevel={"h3"}>Replicas</Title>
        <Text component={"small"}>
          How many copies of a topic will be made for high availability. The
          partitions of each topic can be replicated across a configurable
          number of brokers.
        </Text>
      </TextContent>
      <FormGroup label="Replicas" isRequired fieldId="topic-replicas">
        <NumberInput
          required
          id="topic-replicas"
          name="topic-replicas"
          aria-describedby="topic-replicas-helper"
          value={replicas}
          onChange={(ev) =>
            onChange(parseInt((ev.target as HTMLInputElement).value, 10))
          }
          onMinus={() => onChange(replicas > 1 ? replicas - 1 : replicas)}
          onPlus={() => onChange(replicas + 1)}
          min={1}
          max={maxReplicas}
          validated={
            replicas > 0 && replicas <= maxReplicas ? "default" : "error"
          }
        />
        <FormHelperText>
          <HelperText id={"topic-replicas-helper"}>
            <HelperTextItem variant={"indeterminate"}>
              Replicas are copies of partitions in a topic. Partition replicas
              are distributed over multiple brokers in the cluster to ensure
              topic availability if a broker fails. When a follower replica is
              in sync with a partition leader, the follower replica can become
              the new partition leader if needed. (replication.factor)
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      </FormGroup>
    </FormSection>
  );
}

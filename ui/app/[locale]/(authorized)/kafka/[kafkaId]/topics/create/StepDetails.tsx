import { TopicMutateError } from "@/api/topics/schema";
import { FieldName } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/FieldName";
import { FieldPartitions } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/FieldPartitions";
import { FieldReplicas } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/FieldReplicas";
import { topicMutateErrorToFieldError } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/topicMutateErrorToFieldError";
import { Form } from "@patternfly/react-core";

const legalNameChars = new RegExp("^[a-zA-Z0-9._-]+$");

export function StepDetails({
  name,
  partitions,
  replicas,
  maxReplicas,
  onNameChange,
  onPartitionsChange,
  onReplicasChange,
  error,
}: {
  name: string;
  partitions: number;
  replicas: number;
  maxReplicas: number;
  onNameChange: (name: string) => void;
  onPartitionsChange: (name: number) => void;
  onReplicasChange: (name: number) => void;
  error: TopicMutateError | "unknown" | undefined;
}) {
  const nameInvalid = {
    length: name.trim().length < 3,
    name: [".", ".."].includes(name),
    format: !legalNameChars.test(name),
  };
  const partitionsInvalid = partitions <= 0;
  const replicasInvalid = replicas <= 0 || replicas > maxReplicas;
  const fieldError = topicMutateErrorToFieldError(error, false, [
    "name",
    "numPartitions",
    "replicationFactor",
  ]);

  return (
    <Form>
      <FieldName
        name={name}
        onChange={onNameChange}
        nameInvalid={nameInvalid.name}
        lengthInvalid={nameInvalid.length}
        formatInvalid={nameInvalid.format}
        backendError={fieldError?.field === "name" && fieldError?.error}
      />
      <FieldPartitions
        partitions={partitions}
        onChange={onPartitionsChange}
        invalid={partitionsInvalid}
        backendError={
          fieldError?.field === "numPartitions" && fieldError?.error
        }
      />
      <FieldReplicas
        replicas={replicas}
        maxReplicas={maxReplicas}
        onChange={onReplicasChange}
        showErrors={replicasInvalid}
        backendError={
          fieldError?.field === "replicationFactor" && fieldError?.error
        }
      />
    </Form>
  );
}

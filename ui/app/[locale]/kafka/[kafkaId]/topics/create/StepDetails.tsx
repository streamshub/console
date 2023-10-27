import { TopicCreateError } from "@/api/topics";
import { createErrorToFieldError } from "@/app/[locale]/kafka/[kafkaId]/topics/create/createErrorToFieldError";
import { FieldName } from "@/app/[locale]/kafka/[kafkaId]/topics/create/FieldName";
import { FieldPartitions } from "@/app/[locale]/kafka/[kafkaId]/topics/create/FieldPartitions";
import { FieldReplicas } from "@/app/[locale]/kafka/[kafkaId]/topics/create/FieldReplicas";
import { Form } from "@patternfly/react-core";

const legalNameChars = new RegExp("^[a-zA-Z0-9._-]+$");

export function StepDetails({
  name,
  partitions,
  replicas,
  maxReplicas,
  showError,
  onNameChange,
  onPartitionsChange,
  onReplicasChange,
  error,
}: {
  name: string;
  partitions: number;
  replicas: number;
  maxReplicas: number;
  showError: boolean;
  onNameChange: (name: string) => void;
  onPartitionsChange: (name: number) => void;
  onReplicasChange: (name: number) => void;
  error: TopicCreateError | "unknown" | undefined;
}) {
  const nameInvalid = {
    length: name.trim().length < 3,
    name: [".", ".."].includes(name),
    format: !legalNameChars.test(name),
  };
  const partitionsInvalid = partitions <= 0;
  const replicasInvalid = replicas <= 0 || replicas > maxReplicas;
  const fieldError = createErrorToFieldError(error, false, [
    "name",
    "numPartitions",
    "replicationFactor",
  ]);

  return (
    <Form>
      <FieldName
        name={name}
        onChange={onNameChange}
        showErrors={showError}
        nameInvalid={nameInvalid.name}
        lengthInvalid={nameInvalid.length}
        formatInvalid={nameInvalid.format}
        backendError={fieldError?.field === "name" && fieldError?.error}
      />
      <FieldPartitions
        partitions={partitions}
        onChange={onPartitionsChange}
        invalid={showError && partitionsInvalid}
        backendError={
          fieldError?.field === "numPartitions" && fieldError?.error
        }
      />
      <FieldReplicas
        replicas={replicas}
        maxReplicas={maxReplicas}
        onChange={onReplicasChange}
        showErrors={showError && replicasInvalid}
        backendError={
          fieldError?.field === "replicationFactor" && fieldError?.error
        }
      />
    </Form>
  );
}

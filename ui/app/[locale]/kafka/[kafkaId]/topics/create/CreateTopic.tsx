"use client";
import { ConfigSchemaMap } from "@/api/topics";
import { StepAdvancedConfiguration } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepAdvancedConfiguration";
import { StepName } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepName";
import { StepPartitions } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepPartitions";
import { StepReplicas } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepReplicas";
import { Wizard, WizardStep } from "@patternfly/react-core";
import { useState } from "react";

export function CreateTopic({
  maxReplicas,
  defaultOptions,
}: {
  maxReplicas: number;
  defaultOptions: ConfigSchemaMap;
}) {
  const [showError, setShowError] = useState(false);
  const [name, setName] = useState("");
  const [partitions, setPartitions] = useState(1);
  const [replicas, setReplicas] = useState(maxReplicas);

  const nameInvalid = name.trim() === "";
  const partitionsInvalid = partitions <= 0;
  const replicasInvalid = replicas <= 0 || replicas > maxReplicas;
  return (
    <Wizard
      title="Topic creation wizard"
      onStepChange={() => {
        setShowError(true);
      }}
    >
      <WizardStep
        name="Topic name"
        id="step-name"
        status={showError && nameInvalid ? "error" : "default"}
      >
        <StepName
          name={name}
          onChange={setName}
          invalid={showError && nameInvalid}
        />
      </WizardStep>
      <WizardStep
        name="Partitions"
        id="step-partitions"
        status={showError && partitionsInvalid ? "error" : "default"}
      >
        <StepPartitions
          partitions={partitions}
          onChange={setPartitions}
          invalid={showError && partitionsInvalid}
        />
      </WizardStep>

      <WizardStep
        name="Replicas"
        id="step-replicas"
        status={showError && replicasInvalid ? "error" : "default"}
      >
        <StepReplicas
          replicas={replicas}
          maxReplicas={maxReplicas}
          onChange={setReplicas}
          showErrors={showError && replicasInvalid}
        />
      </WizardStep>
      <WizardStep name="Options" id="step-options">
        <StepAdvancedConfiguration
          options={{}}
          defaultOptions={defaultOptions}
          onChange={() => {}}
          showErrors={showError}
        />
      </WizardStep>
      <WizardStep
        name="Review"
        id="step-review"
        footer={{ nextButtonText: "Finish" }}
      >
        Review step content
      </WizardStep>
    </Wizard>
  );
}

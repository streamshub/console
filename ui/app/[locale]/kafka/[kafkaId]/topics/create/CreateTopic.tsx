"use client";
import { ConfigSchemaMap } from "@/api/topics";
import { StepAdvancedConfiguration } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepAdvancedConfiguration";
import { StepName } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepName";
import { StepPartitions } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepPartitions";
import { StepReplicas } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepReplicas";
import {
  Button,
  Form,
  PageSection,
  useWizardContext,
  Wizard,
  WizardFooterWrapper,
  WizardStep,
} from "@patternfly/react-core";
import { useRouter } from "next/navigation";
import { useState } from "react";

const legalNameChars = new RegExp("^[a-zA-Z0-9._-]+$");

export function CreateTopic({
  maxReplicas,
  defaultOptions,
}: {
  maxReplicas: number;
  defaultOptions: ConfigSchemaMap;
}) {
  const router = useRouter();
  const [showError, setShowError] = useState(false);
  const [name, setName] = useState("");
  const [partitions, setPartitions] = useState(1);
  const [replicas, setReplicas] = useState(maxReplicas);

  const nameInvalid = {
    length: name.trim().length < 3,
    name: [".", ".."].includes(name),
    format: !legalNameChars.test(name),
  };
  const partitionsInvalid = partitions <= 0;
  const replicasInvalid = replicas <= 0 || replicas > maxReplicas;
  return (
    <PageSection type={"wizard"}>
      <Wizard
        title="Topic creation wizard"
        onStepChange={() => {
          setShowError(true);
        }}
        onClose={() => router.back()}
      >
        <WizardStep
          name="Topic details"
          id="step-details"
          status={
            showError &&
            (Object.values(nameInvalid).includes(true) ||
              partitionsInvalid ||
              replicasInvalid)
              ? "error"
              : "default"
          }
          footer={<SkipReviewFooter />}
        >
          <Form>
            <StepName
              name={name}
              onChange={setName}
              showErrors={showError}
              nameInvalid={nameInvalid.name}
              lengthInvalid={nameInvalid.length}
              formatInvalid={nameInvalid.format}
            />
            <StepPartitions
              partitions={partitions}
              onChange={setPartitions}
              invalid={showError && partitionsInvalid}
            />
            <StepReplicas
              replicas={replicas}
              maxReplicas={maxReplicas}
              onChange={setReplicas}
              showErrors={showError && replicasInvalid}
            />
          </Form>
        </WizardStep>
        <WizardStep name="Advanced options" id="step-options">
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
    </PageSection>
  );
}

const SkipReviewFooter = () => {
  const { goToNextStep, goToStepById, close } = useWizardContext();

  return (
    <WizardFooterWrapper>
      <Button isDisabled={true}>Back</Button>
      <Button variant="primary" onClick={goToNextStep}>
        Next
      </Button>
      <Button variant="secondary" onClick={() => goToStepById("step-review")}>
        Skip to review
      </Button>
      <Button variant={"link"} onClick={close}>
        Cancel
      </Button>
    </WizardFooterWrapper>
  );
};

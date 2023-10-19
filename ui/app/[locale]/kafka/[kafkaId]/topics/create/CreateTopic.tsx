"use client";
import {
  ConfigMap,
  NewConfigMap,
  TopicCreateError,
  TopicCreateResponse,
} from "@/api/topics";
import { StepName } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepName";
import { StepOptions } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepOptions";
import { StepPartitions } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepPartitions";
import { StepReplicas } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepReplicas";
import { StepReview } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepReview";
import {
  Button,
  Form,
  PageSection,
  Tooltip,
  useWizardContext,
  Wizard,
  WizardFooterWrapper,
  WizardStep,
} from "@patternfly/react-core";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

const legalNameChars = new RegExp("^[a-zA-Z0-9._-]+$");

export function CreateTopic({
  kafkaId,
  maxReplicas,
  initialOptions,
  onSave,
}: {
  kafkaId: string;
  maxReplicas: number;
  initialOptions: ConfigMap;
  onSave: (
    name: string,
    partitions: number,
    replicas: number,
    options: NewConfigMap,
  ) => Promise<TopicCreateResponse>;
}) {
  const router = useRouter();
  const [showError, setShowError] = useState(false);
  const [name, setName] = useState("");
  const [partitions, setPartitions] = useState(1);
  const [replicas, setReplicas] = useState(maxReplicas);
  const [options, setOptions] = useState<NewConfigMap>({});
  const [pending, startTransition] = useTransition();
  const [errors, setErrors] = useState<
    TopicCreateError | "unknown" | undefined
  >(undefined);

  const handleSave = async () => {
    if (!formInvalid) {
      try {
        setErrors(undefined);
        const result = await onSave(name, partitions, replicas, options);
        if ("errors" in result) {
          setErrors(result);
        } else {
          startTransition(() => {
            router.push(`/kafka/${kafkaId}/topics/${result.data.id}`);
          });
        }
      } catch (e: unknown) {
        setErrors("unknown");
      }
    }
  };

  const nameInvalid = {
    length: name.trim().length < 3,
    name: [".", ".."].includes(name),
    format: !legalNameChars.test(name),
  };
  const partitionsInvalid = partitions <= 0;
  const replicasInvalid = replicas <= 0 || replicas > maxReplicas;
  const formInvalid =
    Object.values(nameInvalid).includes(true) ||
    partitionsInvalid ||
    replicasInvalid;

  return (
    <PageSection type={"wizard"}>
      <Wizard
        title="Topic creation wizard"
        onStepChange={() => {
          setShowError(true);
          setErrors(undefined);
        }}
        onClose={() => router.back()}
      >
        <WizardStep
          name="Topic details"
          id="step-details"
          status={showError && formInvalid ? "error" : "default"}
          footer={<SkipReviewFooter formInvalid={formInvalid} />}
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
        <WizardStep
          name="Options"
          id="step-options"
          footer={{ isNextDisabled: formInvalid }}
        >
          <StepOptions
            options={options}
            initialOptions={initialOptions}
            onChange={setOptions}
          />
        </WizardStep>
        <WizardStep
          name="Review"
          id="step-review"
          footer={
            <ReviewFooter
              formInvalid={formInvalid}
              onSave={handleSave}
              saving={pending}
            />
          }
          isDisabled={formInvalid}
        >
          <StepReview
            name={name}
            nameInvalid={Object.values(nameInvalid).includes(true)}
            partitions={partitions}
            partitionsInvalid={partitionsInvalid}
            replicas={replicas}
            replicasInvalid={replicasInvalid}
            options={options}
            initialOptions={initialOptions}
            error={errors}
          />
        </WizardStep>
      </Wizard>
    </PageSection>
  );
}

const SkipReviewFooter = ({ formInvalid }: { formInvalid: boolean }) => {
  const { goToNextStep, goToStepById, close } = useWizardContext();
  return (
    <WizardFooterWrapper>
      <Button isDisabled={true}>Back</Button>
      <Button variant="primary" onClick={goToNextStep}>
        Next
      </Button>
      <Tooltip
        content={
          "Topic can now be created. You can continue to configure your topic, or you can skip ahead to the review step."
        }
        triggerRef={() => document.getElementById("review-button")!}
      >
        <Button
          variant="tertiary"
          onClick={() => goToStepById("step-review")}
          id={"review-button"}
          isDisabled={formInvalid}
        >
          Review and finish
        </Button>
      </Tooltip>
      <Button variant={"link"} onClick={close}>
        Cancel
      </Button>
    </WizardFooterWrapper>
  );
};

const ReviewFooter = ({
  formInvalid,
  saving,
  onSave,
}: {
  formInvalid: boolean;
  saving: boolean;
  onSave: () => void;
}) => {
  const { goToPrevStep, close } = useWizardContext();
  return (
    <WizardFooterWrapper>
      <Button variant={"secondary"} onClick={goToPrevStep} disabled={saving}>
        Back
      </Button>
      <Button
        variant="primary"
        onClick={onSave}
        disabled={formInvalid || saving}
      >
        Create topic
      </Button>
      <Button variant={"link"} onClick={close} disabled={saving}>
        Cancel
      </Button>
    </WizardFooterWrapper>
  );
};

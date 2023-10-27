"use client";
import {
  ConfigMap,
  NewConfigMap,
  TopicCreateResponse,
  TopicMutateError,
} from "@/api/topics";
import { StepDetails } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepDetails";
import { StepOptions } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepOptions";
import { StepReview } from "@/app/[locale]/kafka/[kafkaId]/topics/create/StepReview";
import {
  Button,
  PageSection,
  Tooltip,
  useWizardContext,
  Wizard,
  WizardFooterWrapper,
  WizardStep,
} from "@patternfly/react-core";
import { useRouter } from "next/navigation";
import { useCallback, useState, useTransition } from "react";

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
    validateOnly: boolean,
  ) => Promise<TopicCreateResponse>;
}) {
  const router = useRouter();
  const [showError, setShowError] = useState(false);
  const [name, setName] = useState("");
  const [partitions, setPartitions] = useState(1);
  const [replicas, setReplicas] = useState(maxReplicas);
  const [options, setOptions] = useState<NewConfigMap>({});
  const [pending, startTransition] = useTransition();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<TopicMutateError | "unknown" | undefined>(
    undefined,
  );

  const handleSave = useCallback(
    async (onSuccess: () => void, validateOnly: boolean) => {
      try {
        setLoading(true);
        const result = await onSave(
          name,
          partitions,
          replicas,
          options,
          validateOnly,
        );
        startTransition(() => {
          setError(undefined);
          if ("errors" in result) {
            setError(result);
          } else {
            if (validateOnly === false) {
              router.push(`/kafka/${kafkaId}/topics/${result.data.id}`);
            } else {
              onSuccess();
            }
          }
        });
      } catch (e: unknown) {
        setError("unknown");
      } finally {
        setLoading(false);
      }
    },
    [kafkaId, name, onSave, options, partitions, replicas, router],
  );

  const formInvalid = error !== undefined;

  return (
    <PageSection type={"wizard"}>
      <Wizard
        title="Topic creation wizard"
        onStepChange={() => {
          setShowError(true);
          setError(undefined);
        }}
        onClose={() => router.back()}
      >
        <WizardStep
          name="Topic details"
          id="step-details"
          footer={
            <SkipReviewFooter
              formInvalid={formInvalid}
              onClick={(success) => handleSave(success, true)}
              loading={pending || loading}
            />
          }
        >
          <StepDetails
            name={name}
            partitions={partitions}
            replicas={replicas}
            maxReplicas={maxReplicas}
            showError={showError}
            onNameChange={setName}
            onPartitionsChange={setPartitions}
            onReplicasChange={setReplicas}
            error={error}
          />
        </WizardStep>
        <WizardStep
          name="Options"
          id="step-options"
          footer={
            <AsyncFooter
              formInvalid={formInvalid || error !== undefined}
              onClick={(success) => handleSave(success, true)}
              loading={pending || loading}
              primaryLabel={"Next"}
            />
          }
        >
          <StepOptions
            options={options}
            initialOptions={initialOptions}
            onChange={setOptions}
            error={error}
          />
        </WizardStep>
        <WizardStep
          name="Review"
          id="step-review"
          footer={
            <AsyncFooter
              formInvalid={formInvalid}
              onClick={(success) => handleSave(success, false)}
              loading={pending || loading}
              primaryLabel={"Create topic"}
            />
          }
          isDisabled={formInvalid}
        >
          <StepReview
            name={name}
            partitions={partitions}
            replicas={replicas}
            options={options}
            initialOptions={initialOptions}
            error={error}
          />
        </WizardStep>
      </Wizard>
    </PageSection>
  );
}

const SkipReviewFooter = ({
  formInvalid,
  onClick,
  loading,
}: {
  formInvalid: boolean;
  onClick: (success: () => void) => void;
  loading: boolean;
}) => {
  const { goToNextStep, goToStepById, close } = useWizardContext();
  return (
    <WizardFooterWrapper>
      <Button isDisabled={true}>Back</Button>
      <Button
        variant="primary"
        onClick={() => onClick(goToNextStep)}
        isLoading={loading}
        isDisabled={loading}
      >
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
          onClick={() => onClick(() => goToStepById("step-review"))}
          id={"review-button"}
          isDisabled={formInvalid || loading}
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

const AsyncFooter = ({
  formInvalid,
  loading,
  primaryLabel,
  onClick,
}: {
  formInvalid: boolean;
  loading: boolean;
  primaryLabel: string;
  onClick: (success: () => void) => void;
}) => {
  const { goToPrevStep, goToNextStep, close } = useWizardContext();
  return (
    <WizardFooterWrapper>
      <Button variant={"secondary"} onClick={goToPrevStep} disabled={loading}>
        Back
      </Button>
      <Button
        variant="primary"
        onClick={() => onClick(goToNextStep)}
        isLoading={loading}
        disabled={formInvalid || loading}
      >
        {primaryLabel}
      </Button>
      <Button variant={"link"} onClick={close} disabled={loading}>
        Cancel
      </Button>
    </WizardFooterWrapper>
  );
};

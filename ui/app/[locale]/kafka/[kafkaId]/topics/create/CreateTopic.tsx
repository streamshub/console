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
import { useRouter } from "@/navigation";
import {
  Button,
  PageSection,
  Tooltip,
  useWizardContext,
  Wizard,
  WizardFooterWrapper,
  WizardStep,
} from "@patternfly/react-core";
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
  const [name, setName] = useState("");
  const [partitions, setPartitions] = useState(1);
  const [replicas, setReplicas] = useState(maxReplicas);
  const [options, setOptions] = useState<NewConfigMap>({});
  const [pending, startTransition] = useTransition();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<TopicMutateError | "unknown" | undefined>(
    undefined,
  );

  const save = useCallback(async () => {
    try {
      setLoading(true);
      setError(undefined);
      const result = await onSave(name, partitions, replicas, options, false);
      startTransition(() => {
        if ("errors" in result) {
          setError(result);
        } else {
          console.log("???", result, result.data.id);
          router.push(`/kafka/${kafkaId}/topics/${result.data.id}`);
        }
      });
    } catch (e: unknown) {
      setError("unknown");
    } finally {
      setLoading(false);
    }
  }, [kafkaId, name, onSave, options, partitions, replicas, router]);

  const validate = useCallback(
    async (success: () => void) => {
      try {
        setLoading(true);
        setError(undefined);
        const result = await onSave(name, partitions, replicas, options, true);
        startTransition(() => {
          if ("errors" in result) {
            setError(result);
          } else {
            success();
          }
        });
      } catch (e: unknown) {
        setError("unknown");
      } finally {
        setLoading(false);
      }
    },
    [name, onSave, options, partitions, replicas],
  );

  const formInvalid = error !== undefined;

  return (
    <PageSection type={"wizard"}>
      <Wizard title="Topic creation wizard" onClose={() => router.back()}>
        <WizardStep
          name="Topic details"
          id="step-details"
          footer={
            <SkipReviewFooter
              formInvalid={formInvalid}
              onClick={(success) => validate(success)}
              loading={pending || loading}
            />
          }
        >
          <StepDetails
            name={name}
            partitions={partitions}
            replicas={replicas}
            maxReplicas={maxReplicas}
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
              nextStepId={"step-review"}
              nextDisabled={formInvalid || error !== undefined}
              onClick={(success) => validate(success)}
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
              nextStepId={""}
              nextDisabled={formInvalid}
              onClick={save}
              loading={pending || loading}
              primaryLabel={"Create topic"}
            />
          }
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
          isDisabled={loading}
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
  nextStepId,
  nextDisabled,
  loading,
  primaryLabel,
  onClick,
}: {
  nextStepId: string;
  nextDisabled: boolean;
  loading: boolean;
  primaryLabel: string;
  onClick: (success: () => void) => void;
}) => {
  const { goToPrevStep, goToStepById, close } = useWizardContext();
  return (
    <WizardFooterWrapper>
      <Button variant={"secondary"} onClick={goToPrevStep} disabled={loading}>
        Back
      </Button>
      <Button
        variant="primary"
        onClick={() =>
          onClick(() => {
            goToStepById(nextStepId);
          })
        }
        isLoading={loading}
        disabled={nextDisabled || loading}
      >
        {primaryLabel}
      </Button>
      <Button variant={"link"} onClick={close} disabled={loading}>
        Cancel
      </Button>
    </WizardFooterWrapper>
  );
};

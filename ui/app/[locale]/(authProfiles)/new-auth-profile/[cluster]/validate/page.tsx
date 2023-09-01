"use client";
import { createAuthProfile } from "@/api/auth";
import {
  Button,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
  PageSection,
  Progress,
  ProgressStep,
  ProgressStepper,
  Split,
  SplitItem,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { CogsIcon } from "@/libs/patternfly/react-icons";
import { Route } from "next";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState, useTransition } from "react";

export default function NewAuthProfilePage({
  params: { cluster },
}: {
  params: { cluster: string };
}) {
  const search = useSearchParams();
  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1">
                Create a new Authorization Profile to access a Cluster
              </Title>
              <Text>
                Brief description of what an Authorization Profile is and works
              </Text>
            </TextContent>
          </SplitItem>
        </Split>
      </PageSection>
      <PageSection>
        <ProgressStepper isCenterAligned aria-label="Basic progress stepper">
          <ProgressStep
            variant="success"
            id="basic-step1"
            titleId="basic-step1-title"
            aria-label="completed step, step with success"
            isCurrent
          >
            Select a Cluster
          </ProgressStep>
          <ProgressStep
            variant="success"
            id="basic-step2"
            titleId="basic-step2-title"
            aria-label="step with info"
          >
            Configure Authentication
          </ProgressStep>
          <ProgressStep
            variant="info"
            id="basic-step3"
            titleId="basic-step3-title"
            aria-label="pending step"
          >
            Validate Connection
          </ProgressStep>
        </ProgressStepper>
      </PageSection>
      <PageSection isFilled>
        <ValidationProgress
          cluster={cluster}
          username={search.get("username") || ""}
        />
      </PageSection>
    </>
  );
}

function ValidationProgress({
  cluster,
  username,
}: {
  cluster: string;
  username: string;
}) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [percentValidated, setPercentValidated] = useState(0);

  const tick = useCallback(() => {
    if (percentValidated < 100) {
      setPercentValidated((prevValue) => prevValue + 20);
    }
  }, [percentValidated]);

  useEffect(() => {
    const interval = setInterval(() => tick(), 300);

    return () => {
      clearInterval(interval);
    };
  }, [tick]);

  return (
    <div className="pf-v5-l-bullseye">
      <EmptyState variant="lg">
        <EmptyStateHeader
          headingLevel="h4"
          titleText={
            percentValidated === 100
              ? "Validation complete"
              : "Validating credentials"
          }
          icon={<EmptyStateIcon icon={CogsIcon} />}
        />
        <EmptyStateBody>
          <Progress
            value={percentValidated}
            measureLocation="outside"
            aria-label="Wizard validation progress"
          />
        </EmptyStateBody>
        <EmptyStateBody>
          Description can be used to further elaborate on the validation step,
          or give the user a better idea of how long the process will take.
        </EmptyStateBody>
        <EmptyStateFooter>
          <EmptyStateActions>
            <Button
              onClick={async () => {
                const profile = await createAuthProfile(cluster, username);
                startTransition(() => {
                  router.push(`/${profile.id}` as Route);
                });
              }}
              isDisabled={percentValidated !== 100 || isPending}
              isLoading={isPending}
            >
              Log to cluster
            </Button>
          </EmptyStateActions>
        </EmptyStateFooter>
      </EmptyState>
    </div>
  );
}

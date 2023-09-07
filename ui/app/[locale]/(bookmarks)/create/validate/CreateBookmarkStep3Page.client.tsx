"use client";
import { createBookmark } from "@/api/bookmarks";
import { Cluster } from "@/api/types";
import {
  Button,
  Card,
  CardBody,
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
  Sidebar,
  SidebarContent,
  SidebarPanel,
  Split,
  SplitItem,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { CogsIcon } from "@/libs/patternfly/react-icons";
import { WarningTriangleIcon } from "@patternfly/react-icons";
import { Route } from "next";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState, useTransition } from "react";

type Props = {
  name: string;
  bootstrapServer: string;
  principal: string;
  cluster: Cluster | undefined;
};

export function CreateBookmarkStep3Page({
  name,
  bootstrapServer,
  principal,
  cluster,
}: Props) {
  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1">
                Create a new Bookmark to access a Cluster
              </Title>
              <Text>
                Brief description of what a Bookmark is and how it works.
              </Text>
            </TextContent>
          </SplitItem>
        </Split>
      </PageSection>
      <PageSection isFilled>
        <Card>
          <CardBody>
            <Sidebar>
              <SidebarPanel>
                <ProgressStepper isVertical aria-label="Basic progress stepper">
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
              </SidebarPanel>
              <SidebarContent>
                <ValidationProgress
                  name={name}
                  bootstrapServer={bootstrapServer}
                  cluster={cluster}
                  principal={principal}
                />
              </SidebarContent>
            </Sidebar>
          </CardBody>
        </Card>
      </PageSection>
    </>
  );
}

function ValidationProgress({
  name,
  bootstrapServer,
  cluster,
  principal,
}: Props) {
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
              ? cluster
                ? "Validation complete"
                : "Validation failed"
              : "Validating credentials"
          }
          icon={
            <EmptyStateIcon
              icon={
                percentValidated === 100 && !cluster
                  ? WarningTriangleIcon
                  : CogsIcon
              }
            />
          }
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
                const profile = await createBookmark({
                  name,
                  bootstrapServer,
                  principal,
                  cluster,
                });
                startTransition(() => {
                  router.push(`/${profile.id}` as Route);
                });
              }}
              isDisabled={
                percentValidated !== 100 || isPending || cluster === undefined
              }
              isLoading={cluster && isPending}
            >
              Log to cluster
            </Button>
            {percentValidated === 100 && cluster === undefined && (
              <Button
                variant={"link"}
                onClick={async () => {
                  const profile = await createBookmark({
                    name,
                    bootstrapServer,
                    principal,
                    cluster,
                  });
                  startTransition(() => {
                    router.push(`/` as Route);
                  });
                }}
                isLoading={isPending}
              >
                Save anyway
              </Button>
            )}
          </EmptyStateActions>
        </EmptyStateFooter>
      </EmptyState>
    </div>
  );
}

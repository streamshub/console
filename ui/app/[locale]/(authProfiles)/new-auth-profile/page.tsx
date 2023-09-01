import { getClusters } from "@/api/auth";
import { ClusterCard } from "@/components/clusterCard";
import {
  Gallery,
  PageSection,
  ProgressStep,
  ProgressStepper,
  Split,
  SplitItem,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";

export default async function NewAuthProfilePage() {
  const clusters = await getClusters();

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
                Brief description of what an Authorization Profile is and how it
                works.
              </Text>
            </TextContent>
          </SplitItem>
        </Split>
      </PageSection>
      <PageSection>
        <ProgressStepper isCenterAligned aria-label="Basic progress stepper">
          <ProgressStep
            variant="info"
            id="basic-step1"
            titleId="basic-step1-title"
            aria-label="completed step, step with success"
            isCurrent
          >
            Select a Cluster
          </ProgressStep>
          <ProgressStep
            variant="pending"
            id="basic-step2"
            titleId="basic-step2-title"
            aria-label="step with info"
          >
            Configure Authentication
          </ProgressStep>
          <ProgressStep
            variant="pending"
            id="basic-step3"
            titleId="basic-step3-title"
            aria-label="pending step"
          >
            Validate Connection
          </ProgressStep>
        </ProgressStepper>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={"Available Clusters"}>
          {clusters.map((c) => (
            <ClusterCard key={c.id} {...c} />
          ))}
        </Gallery>
      </PageSection>
    </>
  );
}

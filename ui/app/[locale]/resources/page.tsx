import { getResources } from "@/api/resources";
import { ButtonLink } from "@/components/buttonLink";
import {
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateIcon,
  Gallery,
  PageSection,
  Split,
  SplitItem,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { PlusCircleIcon } from "@/libs/patternfly/react-icons";
import { ResourceCard } from "./ResourceCard";

export default async function Home() {
  const resources = await getResources("kafka");

  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1" ouiaId={"page-title"}>
                Resources
              </Title>
              <Text ouiaId={"page-description"}>
                Brief description of what a Resource is and how it works.
              </Text>
            </TextContent>
          </SplitItem>
          {resources.length > 0 && (
            <SplitItem>
              <ButtonLink
                href={"/resources/create/kafka"}
                ouiaId={"add-resource-primary"}
              >
                Add a Resource
              </ButtonLink>
            </SplitItem>
          )}
        </Split>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={"Resource"}>
          {resources.map((ap) => (
            <ResourceCard key={ap.id} {...ap} href={`/kafka/${ap.id}`} />
          ))}
        </Gallery>
        {resources.length === 0 && (
          <EmptyState variant={"lg"}>
            <EmptyStateIcon icon={PlusCircleIcon} />
            <Title headingLevel="h2" size="lg">
              No Resources
            </Title>
            <EmptyStateBody>To get started, create a Resource</EmptyStateBody>
            <EmptyStateFooter>
              <EmptyStateActions>
                <ButtonLink variant="primary" href={"/resources/create/kafka"}>
                  Create a Resource
                </ButtonLink>
              </EmptyStateActions>
            </EmptyStateFooter>
          </EmptyState>
        )}
      </PageSection>
    </>
  );
}

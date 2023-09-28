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
import { ClusterCard } from "./ClusterCard";

export default async function Home() {
  const clusters = await getResources("kafka");

  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1" ouiaId={"page-title"}>
                Kafka
              </Title>
              <Text ouiaId={"page-description"}>
                Lorem ipsum dolor sit amet, consectetur adipisicing elit. Cum
                dolor doloremque eligendi, ex fugiat incidunt, inventore ipsam
                natus nobis omnis, quibusdam reiciendis rerum soluta sunt
                suscipit ullam ut vero vitae.
              </Text>
            </TextContent>
          </SplitItem>
          {clusters.length > 0 && (
            <SplitItem>
              <ButtonLink
                href={"/resources/create/kafka"}
                ouiaId={"add-resource-primary"}
              >
                Add a Kafka Cluster Resource
              </ButtonLink>
            </SplitItem>
          )}
        </Split>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={"Authorization Profiles"}>
          {clusters.map((ap) => (
            <ClusterCard key={ap.id} {...ap} href={`/kafka/${ap.id}`} />
          ))}
        </Gallery>
        {clusters.length === 0 && (
          <EmptyState variant={"lg"}>
            <EmptyStateIcon icon={PlusCircleIcon} />
            <Title headingLevel="h2" size="lg">
              No Kafka Cluster Resources
            </Title>
            <EmptyStateBody>
              To get started, create a Kafka Cluster Resource
            </EmptyStateBody>
            <EmptyStateFooter>
              <EmptyStateActions>
                <ButtonLink variant="primary" href={"/resources/create/kafka"}>
                  Create a Kafka Cluster Resource
                </ButtonLink>
              </EmptyStateActions>
            </EmptyStateFooter>
          </EmptyState>
        )}
      </PageSection>
    </>
  );
}
